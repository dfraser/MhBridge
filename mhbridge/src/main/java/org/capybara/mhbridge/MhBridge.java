package org.capybara.mhbridge;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.reficio.ws.client.TransmissionException;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCServer;

public class MhBridge
{

	private final Logger log = Logger.getLogger(MhBridge.class);
	
	private OSCServer c;
	
//	private ConcurrentMap<String, Double> state = new ConcurrentHashMap<>();

	private static MisterHouseSoap mhc;

	private int oscPort;

	private int oscReplyPort;

	private String soapEndpoint;

	public static void main( String[] args ) throws IOException
    {
		MhBridge app = new MhBridge();
		app.run();
    }
	
	public void run() throws IOException {
		log.info("MhBridge starting...");
		// load properties
		Properties properties = new Properties();
		InputStream in = getClass().getResourceAsStream("/mhbridge.properties");
		if (in == null) {
			log.fatal("unable to load properties");
			return;
		}
		properties.load(in);
		in.close();
		
		soapEndpoint = properties.getProperty("misterHouseSoapEndpoint");
		oscPort = Integer.parseInt(properties.getProperty("oscPort"));
		oscReplyPort = Integer.parseInt(properties.getProperty("oscReplyPort"));
		log.info("soap endpoint: "+soapEndpoint);
		log.info("osc listen port: "+oscPort);
		log.info("osc reply port: "+oscReplyPort);
		
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
		        			try {
		        				Double value = new Double((Float)m.getArg(0));
		        				if (value != 1.0) {
		        					log.debug("skipping non-1.0 message");
		        					return;
		        				}
		        				InetSocketAddress rxAddr = (InetSocketAddress) addr;
		        				InetSocketAddress txAddr = new InetSocketAddress(rxAddr.getAddress(), oscReplyPort);
			        			setBusy(true, txAddr);
			        			handleMessage(m.getName(),value);
			        			Thread.sleep(100);			
			        			setBusy(false, txAddr);
		        			} catch(Exception e) {
		        				log.error("error handling message: "+e.getMessage(),e);
		        			}
		        		}
		        	}
		        }
		    });
		 
         c.start();
         
         log.info("ready & running.");
         while (true) {
        	 try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}   	 
         }
         c.stop();
     
	}
	
	
	public void handleMessage(String key, Double value) {
		if (value != 1.0) {
			return;
		}
		// parse the osc name into the misterhouse item
		log.debug("key "+key+" new value: "+value);
		String keyParts[] = key.split("/");
		String mhItem;
		String mhValue;
		if (keyParts.length > 3) {
			mhItem = keyParts[2];
			log.debug("item: "+mhItem);
			mhValue = keyParts[3];
		} else {
			return;
		}
		
		int tries = 2;
		while (tries > 0) {
			try {
				mhc.control(mhItem,mhValue);
				break;
			} catch (TransmissionException e) {
				log.error("transmission error: "+e.getMessage(),e);
			}
			log.debug("retrying...");
			tries--;
		}


	}
	
	public void setBusy(boolean busy, SocketAddress addr) throws IOException {
		Double[] args;
		if (busy) {
			args = new Double[] { 1.0 };
		} else {
			args = new Double[] { 0.0 };
		}
		log.debug("sending busy: "+args[0]);
		c.send(new OSCMessage("/busy", args), addr);
	}
}
