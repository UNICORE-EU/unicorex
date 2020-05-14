package de.fzj.unicore.uas.impl.bp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.ggf.baseprofile.FinalWSResourceInterfaceDocument;
import org.ggf.baseprofile.ResourceEndpointReferenceDocument;
import org.ggf.baseprofile.ResourcePropertyNamesDocument;
import org.ggf.baseprofile.WSResourceInterfacesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

/**
 * Helper for supporting the OGSA baseprofile 1.0
 * 
 * @author schuller
 */
public class BPSupportImpl implements Serializable{
  
	private static final long serialVersionUID = 123L;
	
	private Set<QName> WSResourceInterfaces;

	public static final QName RPWsResourceInterfaces=WSResourceInterfacesDocument.type.getDocumentElementName();
	
	public static final QName RPFinalWSResourceInterface=FinalWSResourceInterfaceDocument.type.getDocumentElementName();
	
	public static final QName RPResourcePropertyNames=ResourcePropertyNamesDocument.type.getDocumentElementName();
	
	public static final QName RPResourceEndpointReference=ResourceEndpointReferenceDocument.type.getDocumentElementName();
	
	public BPSupportImpl() {
		WSResourceInterfaces=new HashSet<QName>();
	}

	public void addWSResourceInterface(QName q){
		WSResourceInterfaces.add(q);
	}
	
	public WSResourceInterfacesDocument getWSResourceInterfaces(){
		XmlObject o=insert(WSResourceInterfacesDocument.type.getDocumentElementName(),WSResourceInterfaces,"bpri");
		WSResourceInterfacesDocument d;
		try {
			d = WSResourceInterfacesDocument.Factory.parse(o.newInputStream());
			return d;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Set<QName> getWSResourceInterfacesList() {
		return WSResourceInterfaces;
	}
	
	public FinalWSResourceInterfaceDocument getFinalResourceInterfaceRP(QName portType)throws Exception{
		Set<QName>names=new HashSet<QName>();
		names.add(portType);
		XmlObject o=insert(FinalWSResourceInterfaceDocument.type.getDocumentElementName(), names, "bprp");
		return FinalWSResourceInterfaceDocument.Factory.parse(o.newInputStream());
	}
	
	/**
	 * make the list of resourceproperty qnames
	 */
	public static ResourcePropertyNamesDocument getRPNamesProperty(Set<QName>names) throws Exception{
		XmlObject o=insert(ResourcePropertyNamesDocument.type.getDocumentElementName(), names, "bprp");
		return ResourcePropertyNamesDocument.Factory.parse(o.newInputStream());
	}
	
	/**
	 * make the endpoint reference property
	 */
	public static ResourceEndpointReferenceDocument getResourceEndpointReferenceRP(EndpointReferenceType epr) throws Exception{
		ResourceEndpointReferenceDocument resourceEPR=ResourceEndpointReferenceDocument.Factory. newInstance();
		resourceEPR.setResourceEndpointReference(epr);
		return resourceEPR;
	}
	
	
	//mess with namespace prefixes and QName lists
	private static XmlObject insert(QName elementName,Set<QName>names, String prefixBase){
		XmlObject o=XmlObject.Factory.newInstance();
		XmlCursor c=o.newCursor();
		Map<String,String>nsMap=new HashMap<String,String>();
		c.toNextToken();
		c.beginElement(elementName);
		int i=1;
		StringBuffer sb=new StringBuffer();
		for(QName q: names){
			String ns=q.getNamespaceURI();
			String prefix;
			if(nsMap.containsKey(ns)){
				prefix=nsMap.get(ns);
			}
			else{
				prefix = prefixBase+i;
				nsMap.put(ns, prefix);
				i++;
			}
			c.insertNamespace(prefix,ns);
			if(i>0)sb.append(" ");
			sb.append(prefix+":"+q.getLocalPart());
		}
		c.insertChars(sb.toString());
		c.dispose();
		return o;
	}
	
}
