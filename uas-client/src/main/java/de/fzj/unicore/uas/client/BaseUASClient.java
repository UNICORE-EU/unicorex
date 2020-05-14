/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.uas.client;

import org.unigrids.services.atomic.types.SecurityDocument;
import org.unigrids.services.atomic.types.SecurityType;
import org.unigrids.services.atomic.types.VOType;
import org.unigrids.services.atomic.types.VersionDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.InvalidResourcePropertyQNameFault;
import eu.unicore.util.httpclient.IClientConfiguration;
/**
 * A base client for the UNICORE WS(RF) services.
 * 
 * @author schuller
 */
public class BaseUASClient extends BaseWSRFClient {

	/**
	 * create a UAS client using the supplied security properties
	 * 
	 * @param url - URL to connect to
	 * @param epr - EPR of the target service
	 * @param sec - the security settings to use
	 */
	public BaseUASClient(String url, EndpointReferenceType epr, IClientConfiguration sec) throws Exception{
		super(url, epr, sec);
	}

	/**
	 * create a UAS client using the supplied security properties
	 * @param epr - EPR of the target service
	 * @param sec - security settings to use
	 */
	public BaseUASClient(EndpointReferenceType epr, IClientConfiguration sec) throws Exception{
		super(epr.getAddress().getStringValue(), epr, sec);
	}

	/**
	 * gets the U/X server version, or <code>null</code> if not set.
	 * All servers since 1.3.0 publish the version
	 * 
	 * @throws Exception
	 * @since 1.3.0
	 */
	public String getServerVersion()throws Exception{
		try{
			String verRP=getResourceProperty(VersionDocument.type.getDocumentElementName());
			if(verRP!=null){
				VersionDocument vD=VersionDocument.Factory.parse(verRP);
				return vD.getVersion();
			}
		}catch(InvalidResourcePropertyQNameFault inv){
			//OK, this means that the server does not publish the version
		}
		return null;
	}

	/**
	 * get the full {@link SecurityType} XML that has all the details about
	 * Xlogins, Xgroups, CAs, VOs etc<br/>
	 * 
	 * Usually it is more convenient to use the dedicated methods like getAcceptedCAs()
	 * @throws Exception
	 */
	public SecurityType getSecurityInfo()throws Exception{
		SecurityDocument sd=SecurityDocument.Factory.parse(
				getResourceProperty(SecurityDocument.type.getDocumentElementName()));
		return sd!=null?sd.getSecurity():SecurityType.Factory.newInstance();
	}

	/**
	 * get the Xlogins the current client has on the remote site
	 * @return non-null array of logins
	 * @throws Exception 
	 */
	public String[] getXlogins() throws Exception{
		String[] srs=new String[0];
		SecurityType secT=getSecurityInfo();
		if(secT!=null && secT.getClientValidXlogins()!=null){
			srs=secT.getClientValidXlogins().getXloginArray();
		}
		return srs;
	}

	/**
	 * get the user's groups on the remote machine
	 * @return non-null array of groups
	 * @throws Exception
	 */
	public String[] getXgroups()throws Exception{
		String[] srs=new String[0];
		SecurityType secT=getSecurityInfo();
		if(secT!=null && secT.getClientValidXgroups()!=null){
			srs=secT.getClientValidXgroups().getXgroupArray();
		}
		return srs;
	}

	/**
	 * get the CAs accepted by the remote server
	 * @return non-null array of CA distinguished names
	 * @throws Exception
	 */
	public String[] getAcceptedCAs()throws Exception{
		String[] srs=new String[0];
		SecurityType secT=getSecurityInfo();
		if(secT!=null) {
			if (secT.getAcceptedCAs()!=null) {
				srs=secT.getAcceptedCAs().getAcceptedCAArray();
			} else if (secT.getTrustedCAArray() != null) {
				/*
				 * pre 6.5 server
				 */
				return secT.getTrustedCAArray();
			}
		}
		return srs;
	}

	/**
	 * get the VOs accepted by the remote server
	 * @return non-null array of VOs
	 * @throws Exception
	 */
	public VOType[] getAcceptedVOs()throws Exception{
		VOType[] srs=new VOType[0];
		SecurityType secT=getSecurityInfo();
		if(secT!=null && secT.getAcceptedVOs()!=null) {
			srs=secT.getAcceptedVOs().getVOArray();
		}
		return srs;
	}

	/**
	 * check whether the server version is at least the required one
	 * @param required - the required version
	 * @return <code>true</code> if the server version is at least the required version
	 * @throws Exception
	 * @since 1.4.1
	 */
	public synchronized boolean checkVersion(String required)throws Exception{
		String v=getServerVersion();
		return compareVersions(required, v);
	}

	static boolean compareVersions(String required, String actual){
		return required.compareTo(actual)<=0 || "DEVELOPMENT".equalsIgnoreCase(actual) ;
	}

}
