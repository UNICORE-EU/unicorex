/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas;

import java.util.Properties;

import de.fzj.unicore.wsrflite.security.ContainerSecurityProperties;
import de.fzj.unicore.wsrflite.server.ContainerHttpServerProperties;
import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

public class TestSecConfigs
{
	public static DefaultClientConfiguration getClientSecurityCfg(boolean clientAuthn) throws Exception{
		String certs="src/test/resources/secure/user-keystore.jks";
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
	
	public static Properties getKernelProperties() {
		Properties k = new Properties();
		String pfx = ContainerSecurityProperties.PREFIX;
		k.setProperty(pfx+ContainerSecurityProperties.PROP_SSL_ENABLED, "true");
		k.setProperty(ContainerHttpServerProperties.PREFIX+ContainerHttpServerProperties.REQUIRE_CLIENT_AUTHN, "true");
		k.setProperty(pfx+CredentialProperties.DEFAULT_PREFIX+CredentialProperties.PROP_LOCATION, "src/test/resources/secure/server-keystore.p12");
		k.setProperty(pfx+CredentialProperties.DEFAULT_PREFIX+CredentialProperties.PROP_PASSWORD, "the!njs");
		k.setProperty(pfx+CredentialProperties.DEFAULT_PREFIX+CredentialProperties.PROP_FORMAT, "pkcs12");
		k.setProperty(pfx+TruststoreProperties.DEFAULT_PREFIX+TruststoreProperties.PROP_TYPE, "directory");
		k.setProperty(pfx+TruststoreProperties.DEFAULT_PREFIX+TruststoreProperties.PROP_DIRECTORY_LOCATIONS+".1", "src/test/resources/secure/*.pem");
		k.setProperty(pfx+ContainerSecurityProperties.PROP_CHECKACCESS, "false");
		k.setProperty(pfx+ContainerSecurityProperties.PROP_GATEWAY_WAIT, "false");
		k.setProperty(pfx+ContainerSecurityProperties.PROP_GATEWAY_AUTHN, "false");
		return k;
	}

}
