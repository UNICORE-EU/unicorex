/*********************************************************************************
 * Copyright (c) 2011 Forschungszentrum Juelich GmbH 
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
 *********************************************************************************/


package de.fzj.unicore.xnjs.io.impl;

import static de.fzj.unicore.xnjs.util.IOUtils.quote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.io.XNJSSocketFactory;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IPlainClientConfiguration;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * mailto: protocol for staging out via email
 * 
 * Can run both on the TSI if a mail script is defined, or on the XNJS using javax.mail
 * 
 * @author schuller
 */
public class MailtoUpload extends AsyncFilemover {

	private static final Logger logger=LogUtil.getLogger(LogUtil.IO,MailtoUpload.class);

	private String subject="Result file from your job";
	private String body="Dear user, please find attached the result file from your job";
	private String address;
	private final IOProperties ioProperties;
	
	/**
	 * create a new "mailto:" file export
	 * 
	 * @param client
	 * @param workingDirectory
	 * @param source
	 * @param target - the mailto: url
	 * @param config
	 */
	public MailtoUpload(Client client, String workingDirectory, String source, String target, XNJS config){
		super(client,workingDirectory,source,target,config);
		info.setProtocol("mailto");
		ioProperties = config.get(IOProperties.class);
		parseMailto();
	}

	private void parseMailto(){
		String target = info.getTarget();
		try{
			String[] queryParts=target.split("\\?",2);
			if(queryParts.length>1){
				address=queryParts[0];
				String query=queryParts[1];
				Map<String,String>headers=asMap(query);
				String reqSubject=headers.get("subject");
				if(reqSubject!=null)subject=reqSubject;
				String reqBody=headers.get("body");
				if(reqBody!=null)body=reqBody;
			}
			else{
				address=target;
			}
		}
		catch(Exception ex){
			logger.warn(Log.createFaultMessage("Error parsing URL: <"+target+">", ex));
		}
	}
	
	private Map<String,String>asMap(String query){
		Map<String,String>result=new HashMap<String,String>();
		String[]headers=query.split("&");
		for(String header: headers){
			String[] hs=header.split("=", 2);
			if(hs.length>1){
				result.put(hs[0],hs[1]);
			}
		}
		return result;
	}
	
	public String getSubject(){
		return subject;
	}
	
	public String getBody(){
		return body;
	}
	
	public final boolean isImport(){
		return false;
	}

	protected void doRun() throws Exception {
		if(ioProperties.getValue(IOProperties.MAIL_WRAPPER)==null){
			runLocally();	
		}
		else{
			super.doRun();
		}
	}

	public String makeCommandline(){
		String mailCmd=ioProperties.getValue(IOProperties.MAIL_WRAPPER);
		return mailCmd+" "+quote(address)+" "+quote(info.getSource())+" "+quote(subject);
	}

	public void runLocally() {
		try{
			postMail(address, subject , body, "unicorex@do-not-reply", info.getSource());
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			reportFailure("Could not perform staging out via email", ex);
		}
	}

	/**
	 * 
	 * @param to - recipient
	 * @param subject
	 * @param text - body of the message
	 * @param whofrom - from address
	 * @param fileName - the filename (for the mail attachment) 
	 */
	public void postMail(String to, String subject, String text, String whofrom, final String fileName)
			throws MessagingException
			{

		Properties props = new Properties();
		String host = ioProperties.getValue("mailHost");
		String port = ioProperties.getValue("mailPort");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		logger.debug("Sending mail via "+host+":"+port);
		
		String user = ioProperties.getValue("mailUser");
		String password = ioProperties.getValue("mailPassword");
		Authenticator auth=null;
		if(user!=null){
			auth=new Authenticator(user, password);
		}
		
		// TODO support for some more of the many other options, see e.g.
		// http://javamail.kenai.com/nonav/javadocs/com/sun/mail/smtp/package-summary.html
		 
		try{
			boolean ssl = ioProperties.getBooleanValue("mailEnableSSL");
			if(ssl){
				props.put("mail.smtp.ssl.enable","true");
				props.put("mail.smtp.socketFactory.fallback", "false");
				SSLSocketFactory sslSocketFactory = new XNJSSocketFactory(configuration).
						getSSLSocketFactoryForSMTP();
				props.put("mail.smtp.ssl.socketFactory", sslSocketFactory);
				IPlainClientConfiguration clientCfg = configuration.get(
						IPlainClientConfiguration.class);
				if (clientCfg.getServerHostnameCheckingMode() == ServerHostnameCheckingMode.FAIL)
					props.put("mail.smtp.ssl.checkserveridentity", "true");
				else if (clientCfg.getServerHostnameCheckingMode() == ServerHostnameCheckingMode.WARN) {
					//TODO - XNJS configuration should be unified with PropertiesHelper, and
					//this should be logged once on startup. 
					logger.info("For SMTP connections the ServerHostnameChecking mode 'WARN' " +
							"is treated as NONE. Only FAIL is effective.");
				}
					
			}
		}catch(Exception ex){
			throw new MessagingException("Could not setup SSL",ex);
		}
		
		Session session = Session.getInstance(props, auth);
		session.setDebug(logger.isDebugEnabled());
		
		MimeMessage message = new MimeMessage(session);

		Address from = new InternetAddress(whofrom);
		message.setFrom(from);
		message.addRecipients(RecipientType.TO, to);
		message.setSubject(subject);

		MimeBodyPart[] mbp = new MimeBodyPart[2];
		Multipart mp = new MimeMultipart();

		mbp[0] = new MimeBodyPart();
		mbp[0].setText(text);
		mp.addBodyPart(mbp[0]);

		mbp[1] = new MimeBodyPart();
		DataSource fds = new DataSource(){

			@Override
			public InputStream getInputStream() throws IOException {
				InputStream attachment=null;
				try{
					if(storageAdapter==null){
						TSI tsi=configuration.getTargetSystemInterface(client);
						tsi.setStorageRoot(workingDirectory);
						attachment=tsi.getInputStream(info.getSource());
					}
					else{
						attachment=storageAdapter.getInputStream(info.getSource());
					}
					return attachment;
				}catch(ExecutionException e){
					throw new IOException(e);
				}
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				throw new IOException();
			}

			@Override
			public String getContentType() {
				return "application/octet-stream";
			}

			@Override
			public String getName() {
				return fileName;
			}

		};

		mbp[1].setDataHandler(new DataHandler(fds));
		mbp[1].setFileName(fileName);
		mp.addBodyPart(mbp[1]);

		message.setContent(mp);
		
		Transport.send(message);

	}
	
	private static class Authenticator extends javax.mail.Authenticator {
		
		private final PasswordAuthentication authentication;
		 
		public Authenticator(String user, String password) {
			authentication = new PasswordAuthentication(user, password);
		}
		
		protected PasswordAuthentication getPasswordAuthentication() {
			return authentication;
		}
	}

}
