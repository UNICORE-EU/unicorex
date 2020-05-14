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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.proxy.ProxyCertificate;
import eu.emi.security.authn.x509.proxy.ProxyCertificateOptions;
import eu.emi.security.authn.x509.proxy.ProxyGenerator;
import eu.unicore.security.wsutil.client.Configurable;
import eu.unicore.security.wsutil.client.DSigOutHandler;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * If needed then a token with a proxy cert in PEM format is inserted into the header.<br/>
 * The properties of the issued proxy can be configured in the security properties passed
 * to the doInit() method: unicore.proxy.{lifetime|keysize} <br/>
 * <p>
 * The handler will regenerate a proxy when half of lifetime of the previous one has passed.
 * <b>This handler is intended for client use</b><br/>
 * 
 * @author schuller
 * @author golbi
 */
public class ProxyCertOutHandler extends AbstractSoapInterceptor implements Configurable {

	private static final Logger logger = Log.getLogger(Log.SECURITY,ProxyCertOutHandler.class);

	protected IClientConfiguration sec;

	protected ProxyCertProperties props;

	private long expiryInstant;

	private String pem;

	//proxy header has this namespace
	public static final String PROXY_NS="http://www.unicore.eu/unicore6";

	//Element name
	public static final String PROXY="Proxy";

	private final static QName headerQName=new QName(PROXY_NS,PROXY);

	public ProxyCertOutHandler() {
		super(Phase.PRE_PROTOCOL);
		getBefore().add(DSigOutHandler.class.getName());
	}


	public synchronized void configure(IClientConfiguration sec) {
		this.sec = sec;
		pem=null;

		props=sec.getConfigurationHandler(ProxyCertProperties.class);
		if(props==null)props=new ProxyCertProperties(new Properties());

		try{
			if(props.isSetFileName()){
				pem=readProxyFromFile(props.getFileValueAsString(ProxyCertProperties.PROXY_FILE, false));
			}
			else{
				pem=generateProxy();
			}
		}
		catch(Exception ce){
			logger.error("Can't create Proxy header: " , ce);
			return;
		}
	}

	protected String readProxyFromFile(String fileName) throws IOException{
		InputStream is=new FileInputStream(fileName);
		try{
			KeyStore ks=CertificateUtils.loadPEMKeystore(is,(char[])null,"none".toCharArray());
			X509Certificate x509 =(X509Certificate)ks.getCertificate("default");
			logger.info("Read proxy from '"+fileName+"' valid till "+x509.getNotAfter());
			expiryInstant=x509.getNotAfter().getTime() - (ProxyCertificateOptions.DEFAULT_LIFETIME/2)*1000;
			return FileUtils.readFileToString(new File(fileName), "UTF-8");
		}
		catch(KeyStoreException ex){
			throw new IOException(ex);
		}
		finally{
			is.close();
		}

	}

	/**
	 * @return pem-encoded proxy or <code>null</code> if not created
	 */
	protected String generateProxy()throws Exception{
		X509Credential credential = sec.getCredential();
		ProxyCertificateOptions param = new ProxyCertificateOptions(credential.getCertificateChain());
		param.setLifetime(props.getLifetime());
		param.setKeyLength(props.getKeysize());	

		ProxyCertificate proxy = ProxyGenerator.generate(param, credential.getKey());

		int lifetime = param.getLifetime();
		expiryInstant=proxy.getCertificateChain()[0].getNotAfter().getTime() - (lifetime/2)*1000;

		ByteArrayOutputStream os = new ByteArrayOutputStream(10240);
		OutputStreamWriter ow=new OutputStreamWriter(os);
		JcaPEMWriter pw=new JcaPEMWriter(ow);
		pw.writeObject(proxy.getCertificateChain()[0]);
		pw.writeObject(proxy.getPrivateKey());
		pw.writeObject(credential.getCertificate());
		pw.close();
		return os.toString("US-ASCII");
	}

	public synchronized void handleMessage(SoapMessage message) {
		//do nothing if not client call
		if(!MessageUtils.isOutbound(message))
			return;

		if (pem == null){
			return;
		}

		if(System.currentTimeMillis()>expiryInstant){
			configure(sec);
		}
		List<Header> h = message.getHeaders();
		try{
			Header header=new Header(headerQName,pem, new JAXBDataBinding(String.class));
			h.add(header);
		}catch(JAXBException e){
			throw new RuntimeException(e);
		}
	}

	// testing use only
	String getPem(){
		return pem;
	}
}


