package org.capybara.mhbridge;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.reficio.ws.client.core.SoapClient;

/**
 * Rough SOAP interface to MisterHouse.
 * 
 * @author dfraser
 *
 */
public class MisterHouseSoap {

	Logger log = Logger.getLogger(MisterHouseSoap.class);
	
	private SoapClient client;
	public MisterHouseSoap(String soapEndpoint) {
		client = SoapClient.builder()
				.endpointUri(soapEndpoint)
				.build();
	}
		
	/**
	 * Update MisterHouse Item State
	 * @param item the MisterHouse item to update
	 * @param value the value to set
	 */
	public void control(String item, String value) {
		    item = StringEscapeUtils.escapeXml(item);
		    value = StringEscapeUtils.escapeXml(value);
		    
		    /*
		     * I tried to do this with fancy SOAP tools, but MisterHouse hated the XML.
		     * This xml came from a test program included with MisterHouse and seems to work.
		     * Bleh.
		     */
		    String env = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><SetItemState xmlns=\"urn:mhsoap\"><param0>$"+item+"</param0><param1>"+value+"</param1></SetItemState></Body></Envelope>";
		    
		    /*
		     * If we don't set the SOAPAction header this way, MisterHouse returns a 500.
		     */
		    String response = client.post("urn:mhsoap#SetItemState",env);
		    log.debug("soap response: "+response);
	}	

}
