package org.capybara.mhbridge;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCServer;

public class MhBridge
{

	private final Logger log = Logger.getLogger(MhBridge.class);
	
	private OSCServer c;
	
	private ConcurrentMap<String, Double> state = new ConcurrentHashMap<>();

	private static MisterHouseSoap mhc;

	public static void main( String[] args ) throws IOException
    {
		MhBridge app = new MhBridge();
		app.run();
    }
	
	public void run() throws IOException {
		// load properties
		Properties properties = new Properties();
		InputStream in = getClass().getResourceAsStream("/mhbridge.properties");
		if (in == null) {
			log.fatal("unable to load properties");
			return;
		}
		properties.load(in);
		in.close();
		
		// TODO: some error checking might be nice
		String soapEndpoint = properties.getProperty("misterHouseSoapEndpoint");
		int oscPort = Integer.parseInt(properties.getProperty("oscPort"));
		
		mhc = new MisterHouseSoap(soapEndpoint);
		try {
			c = OSCServer.newUsing( OSCServer.UDP, oscPort);
		}
		catch( IOException e1 ) {
			log.fatal("error opening osc server",e1);
			return;
		}
		
		/* 
		 * when we receive an osc message, we will update a state map.  we get 
		 * osc messages far more often than we want to update the misterhouse state, 
		 * so we'll just stash them away and update mister house on a timer.
		 * 
		 */
		 c.addOSCListener( new OSCListener() {
		        public void messageReceived( OSCMessage m, SocketAddress addr, long time )
		        {
		        	if (m.getArgCount() > 0) {
		        		if (m.getArg(0) instanceof Float) {
		        			state.put(m.getName(),new Double((Float)m.getArg(0)));
		        		}
		        	}
		        }
		    });
		 
         c.start();
         
         try {
     		Map<String,Double> oldState = new HashMap<>();
        	while (true) {
        		Thread.sleep(500);
        		for (String key : state.keySet()) {
        			if (!state.get(key).equals(oldState.get(key))) {
        				
        				// parse the osc name into the misterhouse item
        				log.debug("key "+key+" new value: "+state.get(key));
        				String keyParts[] = key.split("/");
        				String mhItem;
        				if (keyParts.length > 2) {
							mhItem = keyParts[2];
        					log.debug("item: "+mhItem);
        				} else {
        					continue;
        				}
        				
        				// parse the osc value into misterhouse state, and set it with SOAP api.
        				if (state.get(key) == 1.0) {
        					mhc.control(mhItem, "on");
        				} if (state.get(key) == 0.0) {
        					mhc.control(mhItem, "off");
        				} else {
        					Double val = state.get(key);
        					// convert to percentage
        					int percent = (int) (val.doubleValue() * 100);
        					mhc.control(mhItem, percent+"%");
        				}

        			}
        		}
        		oldState.clear();
        		oldState = new HashMap<>(state);
        	}
		} catch (InterruptedException e) {
			// nothing to do but exit...
			log.debug("interrupted.  cleaning up and exiting...");
		}
        c.stop();
	}
}
