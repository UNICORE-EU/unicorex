/*********************************************************************************
 * Copyright (c) 2008 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DEVELOPED IN THE CONTEXT OF THE OMII-EUROPE PROJECT.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package de.fzj.unicore.uas.security;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.phase.Phase;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.AuthInHandler;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.security.util.AttributeHandlingCallback;
import eu.unicore.services.ws.security.AccessControlHandler;

/**
 * reads a proxy cert (incl. private key) in PEM format from
 * a SOAP header and stores it in the security context
 * 
 * @author schuller
 */
public class ProxyCertInHandler extends AbstractSoapInterceptor implements KernelInjectable
{
	private static final Logger logger = LogUtil.getLogger(LogUtil.SECURITY,ProxyCertInHandler.class);
	
	//proxy header has this namespace
	public static final String PROXY_NS="http://www.unicore.eu/unicore6";

	//Element name
	public static final String PROXY="Proxy";

	private final static QName headerQName=new QName(PROXY_NS,PROXY);
	
	private Kernel kernel;
	
	public void setKernel(Kernel kernel){
		this.kernel=kernel;
	}
	
	public ProxyCertInHandler(Kernel kernel){
		super(Phase.PRE_INVOKE);
		this.kernel=kernel;	
		getAfter().add(AuthInHandler.class.getName());
		getBefore().add(AccessControlHandler.class.getName());
		addAttributeCallback();
	}

	//add callback that extracts attributes when creating a client
	private void addAttributeCallback(){
		kernel.getSecurityManager().addCallback(getCallback());
	}
	
	
	/**
	 * Retrieves the proxy cert from the SOAP Header and puts it
	 * into the security context for later evaluations
	 */
	public void handleMessage(SoapMessage ctx)
	{
		// get the SOAP header
		Header header=ctx.getHeader(headerQName);
		if(header==null)return;
		
		Element proxyElement = (Element) header.getObject();		
		String proxy=proxyElement.getTextContent();
		
		if(logger.isDebugEnabled()){
			logger.debug("Extracted Proxy header:\n"+proxy);		
		}

		// get the security context
		SecurityTokens tokens=((SecurityTokens)ctx.get(SecurityTokens.KEY));		
		if (tokens==null){
			// invalid config
			throw new IllegalStateException("No security tokens found, AuthInHandler is not present or broken");			
		}
		tokens.getUserPreferences().put(PROXY, new String[]{proxy});			
	}
	
	private static AttributeHandlingCallback aac;
	
	private synchronized AttributeHandlingCallback getCallback(){
		if(aac==null){
			aac=new ProxyCertAttributeCallback();
		}
		return aac;
	}
	
	/**
	 * callback functions
	 */
	private static class ProxyCertAttributeCallback implements AttributeHandlingCallback{
		/**
		 * gets the proxy cert from the sec tokens and returns it as an attribute
		 * which will be stored in the client
		 */
		public Map<String,String> extractAttributes(SecurityTokens tokens) {
			Map<String,String> result = new HashMap<String,String>();
			String[] proxy= tokens.getUserPreferences().get(PROXY);
			if(proxy!=null && proxy.length>0){
				result.put(PROXY , proxy[0]);
			}
			return result;
		}
	}
	
}



