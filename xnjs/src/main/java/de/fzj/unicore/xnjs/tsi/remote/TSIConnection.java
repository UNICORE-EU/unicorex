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
 *********************************************************************************/


package de.fzj.unicore.xnjs.tsi.remote;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.util.IOUtils;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * Provides command and data connections to a UNICORE TSI server.
 * 
 * Based on  Sven van den Berghe's ClassicTSIConnection (UNICORE v4)
 * 
 * @author schuller
 */
public class TSIConnection implements AutoCloseable {

	private static final Logger logger=LogUtil.getLogger(LogUtil.TSI,TSIConnection.class);

	private String idLine;

	private static final int BUFSZ = 64000;

	public static final String TSI_OK = "TSI_OK";

	private final Command command;

	private final Data data;

	private final TSIConnectionFactory factory;

	private String tsiVersion;

	private String connectionID;

	private int pingTimeout = 10000;

	private int readTimeout = 180000;

	private final TSIConnector connector;
	
	/**
	 * @param commandSocket - command socket
	 * @param dataSocket - data socket
	 * @param factory - the parent factory
	 * @param tsiHost - the TSI address
	 * @param hostName - the host name (as used in the config file)
	 * @throws IOException
	 */
	public TSIConnection(Socket commandSocket, Socket dataSocket, TSIConnectionFactory factory, TSIConnector connector) throws IOException {
		command = new Command(commandSocket);
		data = new Data(dataSocket);
		this.factory = factory;
		this.connector = connector;
	}

	public InetAddress getTSIAddress(){
		return connector.getAddress();
	}

	public String getTSIHostName(){
		return connector.getHostname();
	}

	/**
	 * Send a message to the TSI.
	 * <p>
	 * The message is sent over the command (character) channel to the TSI.
	 * <p>
	 * Messages to the TSI must conform to the TSI protocol. This method must
	 * append the message terminator (ENDOFMESSAGE\n) to the message when
	 * sending it to the TSI. The TSI will reply with a message when it has
	 * processed the message. This will be terminated by the message terminator
	 * (which should be stripped and not returned). Lines starting TSI_COMMENT
	 * should also be stripped (can be logged).
	 * 
	 * @param message
	 *            The message to send to the TSI. This must conform to the TSI
	 *            protocol.
	 * @return The reply from the TSI.
	 * 
	 * @throws java.io.IOException
	 *             An error occurred during the data send. The TSIConnection is
	 *             unusable.
	 * 
	 */
	public String send(String message) throws java.io.IOException {
		return command.send(message);
	}

	// same without sending the id line
	public String sendNoUser(String message) throws java.io.IOException {
		return command.sendNU(message);
	}

	/**
	 * Read a single line from the TSI command channel.
	 * <p>
	 * Lines starting TSI_COMMENT should be stripped (can be logged). Message
	 * terminators should not be stripped.
	 * 
	 * @return The line read from the TSI.
	 */
	public String getLine() throws java.io.IOException {
		return command.getLine();
	}

	/**
	 * Send data to the TSI over the data channel.
	 * 
	 * @param buffer
	 *            Source of the data.
	 * @param offset
	 *            Where to start reading data from
	 * @param number
	 *            Number of bytes to send to TSI
	 * 
	 */
	public void sendData(byte[] buffer, int offset, int number)
			throws java.io.IOException {
		data.sendData(buffer, offset, number);
	}

	/**
	 * Read data from the TSI over the data channel.
	 * 
	 * @param buffer
	 *            Place to write the data.
	 * @param offset
	 *            Where to start writing data.
	 * @param number
	 *            Number of bytes to read from TSI.
	 */
	public void getData(byte[] buffer, int offset, int number)
			throws java.io.IOException {
		data.getData(buffer, offset, number);
	}

	/**
	 * set the socket timeouts on command and data, as well as the keepAlive feature
	 * 
	 * @param timeout - timeout in millis
	 * @param keepAlive - whether to enable keepAlive
	 */
	public void setSocketTimeouts(int timeout, boolean keepAlive) {
		try{
			data.socket.setSoTimeout(timeout);
			data.socket.setKeepAlive(keepAlive);
			command.socket.setSoTimeout(timeout);
			command.socket.setKeepAlive(keepAlive);
		}catch(Exception ex){}
		this.readTimeout = timeout;
	}

	public void setPingTimeout(int timeout){
		this.pingTimeout = timeout;
	}

	/**
	 * Test the TSIConnection<p/>
	 * 
	 * @return true if the connection is still alive, false otherwise.
	 * 
	 */
	public boolean isAlive() {
		return command != null && command.isAlive();
	}

	private boolean shutDown = false;


	private void done() {
		setIdLine("");
		if(shutDown ){
			return;
		}
		factory.done(this);
	}
	
	public void markTSINodeUnavailable(String message) {
		connector.notOK(message);
	}

	/**
	 * The user of the TSIConnection has finished with the TSIConnection.
	 * <p>
	 * This must <em>always</em> be called as the {@link TSIConnectionFactory}
	 * caches and reuses connections and while allocated TSIConnections are
	 * not available to other users. Also, a TSIConnection holds open sockets
	 * corresponding to TSI processes.
	 * <p>
	 */
	@Override
	public void close() {
		done();
	}
	
	/**
	 * The TSIConnection is no longer required or is unusable.
	 */
	public void shutdown() {
		if(shutDown)return;
		logger.debug("Connection {} shutdown.", getConnectionID());
		shutDown = true;
		command.die();
		data.die();
		factory.notifyConnectionDied();
	}

	public boolean isShutdown() {
		return shutDown;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("TSIConnection[").append(getTSIAddress());
		sb.append(" ").append(command);
		sb.append(" ").append(data);
		sb.append("]");
		return sb.toString();
	}

	private void appendSocketInfo(Socket s, StringBuilder sb){
		try{
			sb.append("peer=").append(s.getInetAddress());
			sb.append(" ssl=").append(s instanceof SSLSocket);
			sb.append(" peerPort=").append(s.getPort());
			sb.append(" localPort=").append(s.getLocalPort());
			sb.append(" timeout=").append(s.getSoTimeout());
			sb.append(" keepAlive=").append(s.getKeepAlive());
		}catch(Exception ex){
			sb.append("ERROR: ").append(ex);
		}
	}

	/**
	 * Sends commands (text) to a particular TSI process.
	 */
	class Command {

		private final Socket socket;

		private final BufferedReader input;

		private final PrintWriter output;

		private boolean checkAlive = true;

		public Command(Socket s) throws IOException {
			socket = s;
			try {
				// build formatted command IO streams on the socket
				input = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
				output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
			} catch (IOException ex) {
				IOUtils.closeQuietly(socket);
				throw ex;
			}
		}

		public String send(String data) throws IOException {
			return _send(data, true);
		}

		public String sendNU(String data) throws IOException {
			return _send(data, false);
		}

		/**
		 * Send the Command String to the execution host. The protocol at this
		 * level is synchronous and instigated only by this side.
		 * 
		 * @param data
		 *            The (character) data to send
		 * @param sendUser
		 *            Append a line with the identity (user,project)?
		 * @return TSI reply, with any comments filtered out
		 */
		private synchronized String _send(String data, boolean sendUser) throws IOException {
			StringBuilder reply = new StringBuilder();
			// Check the outgoing data to prevent users messing with the protocol
			// (e.g. in file names) and acquiring another identity
			//
			// ENDOFMESSAGE will confuse the TSI so exclude
			// #TSI_IDENTITY should appear once only (0 for PING, picked up in
			// TSI)
			// => anything else is suspicious
			if (data.indexOf("ENDOFMESSAGE") > -1)
				throw new IOException(
						"TSI message (user data?) contains ENDOFMESSAGE, this is not allowed");

			// The user ID information is inserted here so the
			// incoming string cannot contain one

			if (data.indexOf("#TSI_IDENTITY") > -1) {
					throw new IOException(
							"TSI message or user's data changes identity far too many times (#TSI_IDENTITY found)");
			}

			try {
				logger.debug("--> [{}] {}", idLine, data);
				output.print(data);
				if (sendUser){
					output.print("\n#TSI_IDENTITY " + idLine + "\n");
				}
				output.print("\nENDOFMESSAGE\n");
				output.flush();
			} catch (Exception ex) {
				shutdown();
				connector.notOK("Error sending request: "+ex.getMessage());
				IOException ioex = new IOException(
						"Failure sending data to the TSI <"+connector.getHostname()+">");
				ioex.initCause(ex);
				throw ioex;
			}
			try{
				// and wait for the reply
				String line = input.readLine();
				while (!line.equals("ENDOFMESSAGE")) {
					reply.append(line).append("\n");
					line = input.readLine();
				}
			}catch(Exception e){
				shutdown();
				connector.notOK("Error reading reply data: "+e.getMessage());
				IOException ioex = new IOException(
						"Failure reading reply data from the TSI <"+connector.getHostname()+">");
				ioex.initCause(e);
				throw ioex;
			}
			logger.debug("<-- {}", reply);
			return reply.toString();
		}

		/**
		 * read a line from the TSI
		 */
		public String getLine() throws IOException {
			String reply = null;
			try {
				reply = input.readLine();
			} catch (IOException ex) {
				shutdown();
				throw ex;
			}
			return reply;
		}

		/**
		 * Command from somewhere to kill this channel
		 */
		public void die() {
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(output);
			IOUtils.closeQuietly(socket);
		}

		/**
		 * do a TSI "ping"
		 */
		public boolean isAlive() {
			if(checkAlive){
				try {
					command.socket.setSoTimeout(pingTimeout);
					_send("#TSI_PING", false);
					drainCommand();
				} catch (Exception ex) {
					return false;
				}
				finally{
					try{
						command.socket.setSoTimeout(readTimeout);
					}catch(Exception ex){};
				}
			}
			return true;
		}
		

		private void drainCommand(){
			try{
				while(command.socket.getInputStream().available()>0){
					command.socket.getInputStream().read();
				}
			}catch(Exception e){}
		}
		

		public String toString(){
			StringBuilder sb=new StringBuilder();
			sb.append("Command[");
			appendSocketInfo(socket,sb);
			sb.append("]");
			return sb.toString();
		}

	}

	/**
	 * Send and receive data from a particular TSI process
	 */
	public class Data {

		private final Socket socket;

		private final OutputStream output;

		private final InputStream input;

		public Data(Socket s) throws IOException {

			socket = s;

			try {
				// build unformatted data IO streams on the socket
				input = new BufferedInputStream(s.getInputStream(), BUFSZ);
				output = new BufferedOutputStream(s.getOutputStream(), BUFSZ);
			} catch (IOException ex) {
				IOUtils.closeQuietly(s);
				throw ex;
			}
		}

		public void sendData(byte[] tosend, int offset, int number)
				throws IOException {

			try {
				if(number>0){
					output.write(tosend, offset, number);
					output.flush();
				}
			} catch (IOException ex) {
				TSIConnection.this.shutdown();
				throw ex;
			}
		}

		// Get a number of bytes from the TSI
		public void getData(byte[] buffer, int offset, int number)
				throws IOException {

			try {
				int read = 0;
				while (read < number) {
					read += input.read(buffer, offset + read, number
							- read);
				}
			} catch (IOException ex) {
				TSIConnection.this.shutdown();
				throw ex;
			}

			return;
		}

		/**
		 * Command from somewhere to kill this channel
		 */
		public void die() {
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(output);
			IOUtils.closeQuietly(socket);;
		}

		public String toString(){
			StringBuilder sb=new StringBuilder();
			sb.append("Data[");
			appendSocketInfo(socket,sb);
			sb.append("]");
			return sb.toString();
		}
	}

	public String getIdLine() {
		return idLine;
	}

	public void setIdLine(String idLine) {
		this.idLine = idLine;
	}


	private static boolean issuedWarning=false;

	public static final String RECOMMENDED_TSI_VERSION = "8.3.0";

	/**
	 * get the TSI version
	 */
	public synchronized String getTSIVersion() throws IOException {
		if(tsiVersion==null){
			tsiVersion = doGetVersion();
		}
		if(tsiVersion!=null && !issuedWarning) {
			issuedWarning = true;
			if(!TSIUtils.compareVersion(tsiVersion, RECOMMENDED_TSI_VERSION)){
				logger.warn("TSI host <{}> runs version <{}> which is outdated. "
						+ "UNICORE will try to work in backwards compatible way, "
						+ "but some features may not work. " +
						"It is strongly suggested to update your TSI.", getTSIHostName(), tsiVersion);
			}
		}
		return tsiVersion;
	}

	//perform a TSI_PING and return the TSI version
	private String doGetVersion()throws IOException{
		String v=null;
		try{
			// use the shorter timeout for this
			command.socket.setSoTimeout(pingTimeout);
			String reply=sendNoUser("#TSI_PING");
			if(reply!=null && reply.length()>0){
				v=reply.trim();
			}
		}catch(IOException se){
			throw se;
		}catch(Exception e){
			throw new IOException(e);
		}
		finally{
			setSocketTimeouts(readTimeout, true);
		}
		return v;
	}

	void setTSIVersion(String version){
		this.tsiVersion=version;
	}

	private long start;

	void startUse(){
		start=System.currentTimeMillis();
		logger.debug("Connection {} checked out", connectionID);
	}

	void endUse(){
		logger.debug("Connection {} was in use for {} millis.", connectionID, (System.currentTimeMillis()-start));
	}

	void setConnectionID(String id){
		this.connectionID=id;
	}

	String getConnectionID(){
		return connectionID;
	}

        TSIConnector getConnector() {
		return connector;
	}
}
