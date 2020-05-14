package de.fzj.unicore.uas.security;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;

public class TestProxyOutHandler {

	@Test
	public void testOutHandler()throws Exception{
		// establish security
		IClientConfiguration sec = getClientSecurityCfg(true);
		ProxyCertOutHandler p=new ProxyCertOutHandler();
		p.configure(sec);
		String pem = p.getPem();
		Assert.assertNotNull(pem);
	}

	@Test
	public void testOutHandlerUsingExistingProxyFile()throws Exception{
		// establish security
		IClientConfiguration sec = getClientSecurityCfg(true);
		ProxyCertOutHandler p=new ProxyCertOutHandler();
		p.configure(sec);

		File tmp=new File("target","proxy-"+System.currentTimeMillis());
		tmp.deleteOnExit();
		String pem=p.getPem();
		FileUtils.writeStringToFile(tmp, pem, "UTF-8");
		
		p=new ProxyCertOutHandler();
		sec = getClientSecurityCfg(true);
		ProxyCertProperties props=new ProxyCertProperties(new Properties());
		props.setProperty(ProxyCertProperties.PROXY_FILE,tmp.getAbsolutePath());
		sec.addConfigurationHandler(props);
		p.configure(sec);
		String newPem=p.getPem();
		Assert.assertTrue("Proxy does not match file content",newPem.equals(pem));
	}
	
	public static DefaultClientConfiguration getClientSecurityCfg(boolean clientAuthn) throws Exception{
		String certs="src/test/resources/user-keystore.jks";
		DefaultClientConfiguration secP=new DefaultClientConfiguration();
		secP.setSslEnabled(true);
		secP.setSslAuthn(clientAuthn);
		secP.setDoSignMessage(true);
		secP.setValidator(new KeystoreCertChainValidator(certs, 
				"the!user".toCharArray(), "jks", -1));
		secP.setCredential(new KeystoreCredential(certs, 
				"the!user".toCharArray(), "the!user".toCharArray(), null, "jks"));
		return secP;
	}
	
	
}
