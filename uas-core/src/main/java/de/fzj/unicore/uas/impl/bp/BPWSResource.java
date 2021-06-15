package de.fzj.unicore.uas.impl.bp;

import javax.xml.namespace.QName;

import eu.unicore.services.ws.WSResource;

/**
 * Base profile enabled WSResource interface. This allows to retrieve BP
 * properties from resource instance at client side, given POJO interface
 * is extended from this interface
 * 
 * TODO move to 'use-cxfclient' module
 * 
 * @author Shiraz Memon
 */
public interface BPWSResource extends WSResource {
	//A list of all Resource property QNames it contains
	public static final QName RPResourcePropertyNames = BPSupportImpl.RPResourcePropertyNames;
	//A list of all known portTypes that were composed together to make a new portType for the wsdl:Service/port
	public static final QName RPWsResourceInterfaces = BPSupportImpl.RPWsResourceInterfaces;
	//The final portType interface QName 
	public static final QName RPFinalWSResourceInterface = BPSupportImpl.RPFinalWSResourceInterface;
	//The wsa:EndpointReference(s) for itself
	public static final QName RPResourceEndpointReference = BPSupportImpl.RPResourceEndpointReference;
}
