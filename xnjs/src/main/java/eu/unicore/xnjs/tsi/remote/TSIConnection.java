package eu.unicore.xnjs.tsi.remote;

import java.io.IOException;

import eu.unicore.xnjs.tsi.remote.server.TSIConnector;

public interface TSIConnection extends AutoCloseable {

	String TSI_OK = "TSI_OK";

	//InetAddress getTSIAddress();

	String getTSIHostName();

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
	String send(String message) throws IOException;

	/**
	 * Read a single line from the TSI command channel.
	 * <p>
	 * Lines starting TSI_COMMENT should be stripped (can be logged). Message
	 * terminators should not be stripped.
	 * 
	 * @return The line read from the TSI.
	 */
	String getLine() throws IOException;

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
	void sendData(byte[] buffer, int offset, int number) throws IOException;

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
	void getData(byte[] buffer, int offset, int number) throws IOException;

	/**
	 * set the read/write timeouts on command and data, as well as the keepAlive feature
	 * 
	 * @param timeout - timeout in millis
	 * @param keepAlive - whether to enable keepAlive
	 */
	void setTimeouts(int timeout, boolean keepAlive);

	/**
	 * set the timeout for a "PING" status check
	 * @param timeout
	 */
	void setPingTimeout(int timeout);

	/**
	 * Check if TSIConnection is OK. will also check the general state
	 * of the parent {@link TSIConnector}, and only if that is fine, it
	 * will run a "ping" to check if this connection is alive <p/>
	 * 
	 * @return <code>true</code> if the connection is OK, <code>false</code> otherwise.
	 */
	boolean isAlive();

	void markTSINodeUnavailable(String message);

	/**
	 * The TSIConnection is no longer required or is unusable.
	 */
	void shutdown();

	boolean isShutdown();

	/**
	 * get the TSI version
	 */
	String getTSIVersion() throws IOException;
	
	/**
	 * check if TSI is at least the minimum required version
	 * @param minRequired
	 */
	public boolean compareVersion(String minRequired);
	
	/**
	 * auto-closeable
	 */
	void close();

	/**
	 * get a description of the target system user we operate as
	 * (for logging purposes only)
	 */
	public String getUserDescription();


	/**
	 * compares versions
	 * @param haveVersion - the current TSI version
	 * @param minRequired - the mininum required version
	 * @return <code>true</code> if version is less than required, <code>false</code> otherwise
	 */
	public static boolean doCompareVersions(String haveVersion, String minRequired) {
		String[] curS = haveVersion.split("\\.");
		String[] reqS = minRequired.split("\\.");
		int[] cur = new int[curS.length];
		int[] req = new int[reqS.length];
		try{
			for (int i=0; i<curS.length; i++)
				cur[i] = Integer.parseInt(curS[i]);
			for (int i=0; i<reqS.length; i++)
				req[i] = Integer.parseInt(reqS[i]);
		}
		catch(NumberFormatException ex){
			return false;
		}
		for (int i=0; i<Math.min(cur.length, req.length); i++) {
			if (cur[i] < req[i])
				return false;
			else if (cur[i] > req[i])
				return true;
		}
		if (cur.length >= req.length)
			return true;
		return false;
	}


}