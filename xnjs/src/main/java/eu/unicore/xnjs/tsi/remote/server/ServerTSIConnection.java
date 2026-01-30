package eu.unicore.xnjs.tsi.remote.server;

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
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocket;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.xnjs.tsi.remote.TSIConnection;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Provides command and data connections to a UNICORE TSI server.
 *
 * Based on  Sven van den Berghe's ClassicTSIConnection (from UNICORE 4)
 *
 * @author schuller
 */
public class ServerTSIConnection implements eu.unicore.xnjs.tsi.remote.TSIConnection {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TSI, TSIConnection.class);

	private String idLine;

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
	 * @param connector - the TSI connector
	 * @throws IOException
	 */
	public ServerTSIConnection(Socket commandSocket, Socket dataSocket, TSIConnectionFactory factory, TSIConnector connector) throws IOException {
		command = new Command(commandSocket);
		data = new Data(dataSocket);
		this.factory = factory;
		this.connector = connector;
	}

	public InetAddress getTSIAddress(){
		return connector.getAddress();
	}

	@Override
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
	@Override
	public String send(String message) throws java.io.IOException {
		return command.send(message);
	}

	/**
	 * Read a single line from the TSI command channel.
	 * <p>
	 * Lines starting TSI_COMMENT should be stripped (can be logged). Message
	 * terminators should not be stripped.
	 * 
	 * @return The line read from the TSI.
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
	public void setTimeouts(int timeout, boolean keepAlive) {
		try{
			data.socket.setSoTimeout(timeout);
			data.socket.setKeepAlive(keepAlive);
			command.socket.setSoTimeout(timeout);
			command.socket.setKeepAlive(keepAlive);
		}catch(Exception ex){}
		this.readTimeout = timeout;
	}

	@Override
	public void setPingTimeout(int timeout){
		this.pingTimeout = timeout;
	}

	/**
	 * Check if TSIConnection is OK. will also check the general state
	 * of the parent {@link TSIConnector}, and only if that is fine, it
	 * will run a "ping" to check if this connection is alive <p/>
	 * 
	 * @return <code>true</code> if the connection is OK, <code>false</code> otherwise.
	 */
	@Override
	public boolean isAlive() {
		return connector.isOK() && command.isAlive();
	}

	private boolean shutDown = false;

	@Override
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
		idLine = "";
		if(!shutDown){
			factory.done(this);
		}
	}
	
	/**
	 * The TSIConnection is no longer required or is unusable.
	 */
	@Override
	public void shutdown() {
		if(!shutDown) {
			logger.debug("Connection {} shutdown.", getConnectionID());
			shutDown = true;
			command.close();
			data.close();
			factory.notifyConnectionDied();
		}
	}

	@Override
	public boolean isShutdown() {
		return shutDown;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("TSIConnection[").append(getTSIHostName());
		sb.append(" ip=").append(getTSIAddress());
		sb.append(" ").append(command);
		sb.append(" ").append(data);
		sb.append("]");
		return sb.toString();
	}
	
	public boolean compareVersion(String minRequired) {
		if(tsiVersion==null)return false;
		return TSIConnection.doCompareVersions(tsiVersion, minRequired);
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

		@SuppressWarnings("resource")
		public Command(Socket socket) throws IOException {
			this.socket = socket;
			try {
				// build formatted command IO streams on the socket
				input = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
				output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
			} catch (IOException ex) {
				IOUtils.closeQuietly(socket);
				throw ex;
			}
		}

		/**
		 * Send the Command String to the TSI and return the reply
		 *
		 * @param data
		 *            The (character) data to send
		 * @return TSI reply, with any comments filtered out
		 */
		public String send(String data) throws IOException {
			StringBuilder reply = new StringBuilder();
			// Check the outgoing data to prevent users messing with the protocol
			String[] forbidden = new String[] {"ENDOFMESSAGE", "#TSI_IDENTITY"};
			for(String s: forbidden) {
				if (data.indexOf(s) > -1) {
					throw new IOException("TSI message or user data contains '"+s+
							"', this is not allowed");
				}
			}
			try {
				logger.debug("--> [{}] {}", idLine, data);
				output.print(data);
				output.print("\n#TSI_IDENTITY " + idLine+"\n");
				output.print("\nENDOFMESSAGE\n");
				output.flush();
			} catch (Exception e) {
				shutdown();
				String msg = Log.getDetailMessage(e);
				connector.notOK(msg);
				throw new IOException("Failure sending request to TSI <" +
						connector.getHostname()+">: "+msg);
			}
			try{
				String line = input.readLine();
				if(line==null) {
					throw new IOException("Unexpected end of stream");
				}
				while (!line.equals("ENDOFMESSAGE")) {
					reply.append(line).append("\n");
					line = input.readLine();
					if(line==null) {
						throw new IOException("Unexpected end of stream");
					}
				}
			}catch(Exception e){
				shutdown();
				String msg = Log.getDetailMessage(e);
				connector.notOK(msg);
				throw new IOException("Failure reading reply from TSI <" +
						connector.getHostname()+">: "+msg);
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

		public void close() {
			IOUtils.closeQuietly(input, output, socket);
		}

		/**
		 * do a TSI "ping"
		 */
		public boolean isAlive() {
			if(checkAlive){
				try {
					command.socket.setSoTimeout(pingTimeout);
					drainCommand();
					send("#TSI_PING");
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

		public Data(Socket socket) throws IOException {
			this.socket = socket;
			try {
				// build unformatted data IO streams on the socket
				input = new BufferedInputStream(socket.getInputStream(), 65536);
				output = new BufferedOutputStream(socket.getOutputStream(), 65536);
			} catch (IOException ex) {
				IOUtils.closeQuietly(socket);
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
				ServerTSIConnection.this.shutdown();
				throw ex;
			}
		}

		// Get a number of bytes from the TSI
		public void getData(byte[] buffer, int offset, int number) throws IOException {
			try {
				int read = 0;
				while (read < number) {
					read += input.read(buffer, offset + read, number
							- read);
				}
			} catch (IOException ex) {
				ServerTSIConnection.this.shutdown();
				throw ex;
			}

			return;
		}

		public void close() {
			IOUtils.closeQuietly(input, output, socket);;
		}

		public String toString(){
			StringBuilder sb=new StringBuilder();
			sb.append("Data[");
			appendSocketInfo(socket,sb);
			sb.append("]");
			return sb.toString();
		}
	}

	@Override
	public String getUserDescription() {
		return idLine;
	}

	public void setUser(String user, String group) {
		this.idLine = user+" "+(group!=null ? group : "NONE");
	}

	private static final Map<String, Boolean> issuedWarnings = new HashMap<>();

	public static final String RECOMMENDED_TSI_VERSION = "10.4.0";

	/**
	 * get the TSI version
	 */
	@Override
	public synchronized String getTSIVersion() throws IOException {
		if(tsiVersion==null){
			tsiVersion = doGetVersion();
		}
		if(tsiVersion!=null) {
			try {
				String tsiHost = getTSIHostName();
				Boolean issuedWarning = issuedWarnings.get(tsiHost);
				if(issuedWarning==null) {
					issuedWarnings.put(tsiHost, Boolean.TRUE);
					if(!compareVersion(RECOMMENDED_TSI_VERSION)){
						logger.warn("TSI host <{}> runs version <{}> which is outdated. "
							+ "Some features may not work as expected. " +
							"It is suggested to update your TSI to version <{}> or higher.",
							tsiHost, tsiVersion, RECOMMENDED_TSI_VERSION);
					}
				}
			}catch(Exception e) {}
		}
		return tsiVersion;
	}

	//perform a TSI_PING and return the TSI version
	private String doGetVersion()throws IOException{
		String v=null;
		try{
			// use the shorter timeout for this
			command.socket.setSoTimeout(pingTimeout);
			String reply = send("#TSI_PING");
			if(reply!=null && reply.length()>0){
				v=reply.trim();
			}
		}catch(IOException se){
			throw se;
		}catch(Exception e){
			throw new IOException(e);
		}
		finally{
			setTimeouts(readTimeout, true);
		}
		return v;
	}

	void setTSIVersion(String version){
		this.tsiVersion=version;
	}
	void setConnectionID(String id){
		this.connectionID=id;
	}

	public String getConnectionID(){
		return connectionID;
	}

    TSIConnector getConnector() {
		return connector;
	}
}
