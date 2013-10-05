package org.capybara.mhbridge;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.reficio.ws.client.core.SoapClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

	public String query(String item) {
	    item = StringEscapeUtils.escapeXml(item);

		/*
		 * I tried to do this with fancy SOAP tools, but MisterHouse hated the XML.
		 * This xml came from a test program included with MisterHouse and seems to work.
		 * Bleh.
		 */
		String env = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><GetItemState xmlns=\"urn:mhsoap\"><param0>"+item+"</param0></GetItemState></Body></Envelope>";

		/*
		 * If we don't set the SOAPAction header this way, MisterHouse returns a 500.
		 */
		String response = client.post("urn:mhsoap#GetItemState",env);
		log.debug("soap response: "+response);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
	        InputSource is = new InputSource(new StringReader(response));

			Document doc = builder.parse(is);

			XPathFactory pathFactory = XPathFactory.newInstance();
			XPath path = pathFactory.newXPath();
			XPathExpression expression;
			expression = path.compile("//*[local-name()='GetItemStateResponse']/*");
			NodeList nodeList = (NodeList) expression.evaluate(doc, XPathConstants.NODESET);
			if (nodeList.getLength() > 0) {
				Node node = nodeList.item(0).getFirstChild();
				String value = node.getNodeValue();
				log.debug("found node: "+node.getNodeName()+" value: "+value);
				return value;
			}
		} catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException e) {
			log.error("unable to parse soap response: "+e.getMessage(),e);
		}
		return "";
	}

}
