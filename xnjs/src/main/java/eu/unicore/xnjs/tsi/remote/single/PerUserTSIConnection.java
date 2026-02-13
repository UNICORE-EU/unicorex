package eu.unicore.xnjs.tsi.remote.single;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.Session;

import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.xnjs.tsi.remote.TSIConnection;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Provides TSI command and data channel via a single in/out stream
 * 
 * @author schuller
 */
public class PerUserTSIConnection implements TSIConnection {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TSI, TSIConnection.class);

	private Client client;

	private BufferedReader input;

	private PrintWriter output;

	private final PerUserTSIConnectionFactory factory;

	private String tsiVersion;

	private String connectionID;

	private int timeout = 180000;

	private final Connector connector;

	private Closeable closeCallback;

	private final Session sshSession;

	/**
	 * @param factory - the parent factory
	 * @param connector - the TSI connector
	 * @param client
	 * @throws IOException
	 */
	public PerUserTSIConnection(Session sshSession, PerUserTSIConnectionFactory factory, Connector connector, Client client)
			throws IOException {
		this.sshSession = sshSession;
		this.factory = factory;
		this.connector = connector;
		this.client = client;
		this.connectionID = getUserDescription()+"@"+connector.getHostname();
	}

	public void setInput(InputStream in) throws IOException {
		this.input  = new BufferedReader(new InputStreamReader(in,"UTF-8"));
	}

	public void setOutput(OutputStream out) throws IOException {
		this.output = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
	}

	public void setCloseCallback(Closeable closeable) {
		this.closeCallback = closeable;
	}

	public Session getSession() {
		return sshSession;
	}

	/**
	 * the Unix user name on the remote system
	 */
	public Client getClient(){
		return client;
	}

	/**
	 * make ready for use
	 */
	public void activate() throws TSIUnavailableException {
		try{
			connector.activate(this);
		}catch(Exception e) {
			throw new TSIUnavailableException("Cannot activate connection for "
					+getUserDescription(), e);
		}
	}

	@Override
	public String getTSIHostName(){
		return connector.getHostname();
	}

	@Override
	public String getUserDescription() {
		return client.getSelectedXloginName();
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
			String user = client.getSelectedXloginName();
			logger.debug("--> [{}] {}", user, data);
			output.println(data);
			output.println("#TSI_IDENTITY " + user + " NONE");
			output.println("ENDOFMESSAGE");
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
	
	@Override
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

	private static final String _begin_data = "---BEGIN DATA BASE64---";
	private static final String _end_data = "---END DATA---";
	
	@Override
	public void sendData(byte[] buffer, int offset, int number) throws IOException {
		String user = client.getSelectedXloginName();
		logger.debug("--> [{}] {}", user, _begin_data);
		output.println("---BEGIN DATA BASE64---");
		String base64;
		if(number==buffer.length) {
			base64 = Base64.getEncoder().encodeToString(buffer);
		}
		else {
			byte[]buf = new byte[number];
			System.arraycopy(buffer, 0, buf, 0, number);
			base64 = Base64.getEncoder().encodeToString(buf);
		}
		logger.debug("--> [{}] ({} bytes of encoded data)", user, number);
		output.println(base64);
		logger.debug("--> [{}] {}", user, _end_data);
		output.println(_end_data);
		output.flush();
	}

	@Override
	public void getData(byte[] buffer, int offset, int number) throws IOException {
		String line = getLine();
		logger.debug("<-- {}", line);
		if(!line.startsWith("---BEGIN DATA "))throw new IOException("TSI protocol error - expected BEGIN DATA tag");
		StringBuilder sb = new StringBuilder();
		do {
			line = getLine();
			if("---END DATA---".equals(line))break;
			if(sb.length()>0)sb.append("\n");
			sb.append(line);
		}while(line!=null);
		byte[]buf = Base64.getDecoder().decode(sb.toString());
		logger.debug("<-- <{}> bytes of encoded data", buf.length);
		logger.debug("<-- {}", _end_data);
		if(buf.length!=number)throw new IOException("TSI protocol error - expected <"+number+"> bytes, got <"+buf.length+">");
		System.arraycopy(buf, 0, buffer, offset, number);
	}

	@Override
	public void setTimeouts(int timeout, boolean keepAlive) {
		try{
	
		}catch(Exception ex){}
		this.timeout = timeout;
	}

	@Override
	public void setPingTimeout(int timeout){
		// NOP
	}

	@Override
	public boolean isAlive() {
		return factory.isTesting() || sshSession.isConnected();
	}

	private boolean shutDown = false;

	@Override
	public void markTSINodeUnavailable(String message) {
		connector.notOK(message);
	}

	@Override
	public void close() {
		IOUtils.closeQuietly(closeCallback);
		if(!shutDown){
			factory.done(this);
		}
	}

	@Override
	public void shutdown() {
		if(!shutDown) {
			logger.debug("Connection {} shutdown.", connectionID);
			shutDown = true;
			IOUtils.closeQuietly(input, output);
			if(sshSession!=null){
				try{
					sshSession.disconnect();
				}catch(Exception e) { /*ignored*/ }
			}
			factory.notifyConnectionDied();
		}
	}

	@Override
	public boolean isShutdown() {
		return shutDown;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("TSIConnection[");
		sb.append(getUserDescription()).append("@").append(getTSIHostName());
		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean compareVersion(String minRequired) {
		if(tsiVersion==null)return false;
		return TSIConnection.doCompareVersions(tsiVersion, minRequired);
	}


	private static final Map<String, Boolean> issuedWarnings = new HashMap<>();

	public static final String RECOMMENDED_TSI_VERSION = "10.5.0";

	/**
	 * get the TSI version
	 */
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

	// perform a TSI_PING and return the TSI version
	private String doGetVersion()throws IOException{
		String v = null;
		try{
			String reply = send("#TSI_PING");
			if(reply!=null && reply.length()>0){
				v = reply.trim();
			}
		}catch(IOException se){
			throw se;
		}catch(Exception e){
			throw new IOException(e);
		}
		finally{
			setTimeouts(timeout, true);
		}
		return v;
	}

}