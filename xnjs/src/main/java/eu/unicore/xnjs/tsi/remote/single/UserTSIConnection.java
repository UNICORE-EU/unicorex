package eu.unicore.xnjs.tsi.remote.single;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.xnjs.tsi.remote.TSIConnection;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Provides TSI command and data channel via a single in/out stream
 * 
 * @author schuller
 */
public class UserTSIConnection implements TSIConnection {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TSI,UserTSIConnection.class);

	private final String idLine;

	private final BufferedReader input;

	private final PrintWriter output;
	
	private final SSHTSIConnectionFactory factory;

	private String tsiVersion;

	private String connectionID;

	private int readTimeout = 180000;

	private final Connector connector;

	private final Closeable closeCallback;

	/**
	 * @param in - input
	 * @param out - output
	 * @param factory - the parent factory
	 * @param connector - the TSI connector
	 * @throws IOException
	 */
	public UserTSIConnection(InputStream in, OutputStream out, SSHTSIConnectionFactory factory, Connector connector, String user,
			Closeable closeCallback)
			throws IOException {
		input  = new BufferedReader(new InputStreamReader(in,"UTF-8"));
		output = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		this.factory = factory;
		this.connector = connector;
		this.idLine = user;
		this.connectionID = user+"@"+connector.getHostname();
		this.closeCallback = closeCallback;
	}

	@Override
	public String getTSIHostName(){
		return connector.getHostname();
	}

	@Override
	public String getUserDescription() {
		return idLine;
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
			output.println(data);
			output.println("#TSI_IDENTITY " + idLine);
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

	@Override
	public void sendData(byte[] buffer, int offset, int number) throws IOException {
		output.println("---BEGIN DATA BASE64---");
		String base64;
		if(number==buffer.length) {
			base64 = Base64.encodeBase64String(buffer);
		}
		else {
			byte[]buf = new byte[number];
			System.arraycopy(buffer, 0, buf, 0, number);
			base64 = Base64.encodeBase64String(buf);
		}
		output.println(base64);
		output.println("---END DATA---");
		output.flush();
	}

	@Override
	public void getData(byte[] buffer, int offset, int number) throws IOException {
		String line = getLine();
		if(!line.startsWith("---BEGIN DATA "))throw new IOException("TSI protocol error - expected BEGIN DATA tag");
		StringBuilder sb = new StringBuilder();
		do {
			line = getLine();
			if("---END DATA---".equals(line))break;
			sb.append(line).append("\n");
		}while(line!=null);
		byte[]buf = Base64.decodeBase64(sb.toString().getBytes());
		if(buf.length!=number)throw new IOException("TSI protocol error - expected <"+number+"> bytes, got <"+buf.length+">");
		System.arraycopy(buf, 0, buffer, offset, number);
	}

	@Override
	public void setSocketTimeouts(int timeout, boolean keepAlive) {
		try{
	
		}catch(Exception ex){}
		this.readTimeout = timeout;
	}

	@Override
	public void setPingTimeout(int timeout){
		// NOP
	}

	@Override
	public boolean isAlive() {
		return connector.isOK();
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
		IOUtils.closeQuietly(closeCallback);
		if(!shutDown){
			factory.done(this);
		}
		IOUtils.closeQuietly(closeCallback);
	}

	@Override
	public void shutdown() {
		if(!shutDown) {
			logger.debug("Connection {} shutdown.", connectionID);
			shutDown = true;
			IOUtils.closeQuietly(input, output);
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
		sb.append(idLine).append("@").append(getTSIHostName());
		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean compareVersion(String minRequired) {
		if(tsiVersion==null)return false;
		return TSIConnection.doCompareVersions(tsiVersion, minRequired);
	}


	private static final Map<String, Boolean> issuedWarnings = new HashMap<>();

	public static final String RECOMMENDED_TSI_VERSION = "9.1.0";

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
			setSocketTimeouts(readTimeout, true);
		}
		return v;
	}

}