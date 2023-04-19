package eu.unicore.client.data;

public interface UFTPConstants {
	/** 
	 * client host 
	 */
	public static final String PARAM_CLIENT_HOST="uftp.client.host";

	/**
	 * requested number of parallel data streams
	 */
	public static final String PARAM_STREAMS="uftp.streams";

	/**
	 * server host 
	 */
	public static final String PARAM_SERVER_HOST="uftp.server.host";

	/**
	 * server port 
	 */
	public static final String PARAM_SERVER_PORT="uftp.server.port";

	/**
	 * secret, i.e. authz token that the server should expect for authorising the
	 * present transfer
	 */
	public static final String PARAM_SECRET="uftp.secret";

	/** 
	 * key for encrypting and decrypting data. This will be generated on the server
	 * if the client requests it by setting PARAM_ENABLE_ENCRYPTION to "true" when creating the transfer.
	 */
	public static final String PARAM_ENCRYPTION_KEY="uftp.encryption_key";

	/**
	 * enable encryption by setting this to "true"
	 */
	public static final String PARAM_ENABLE_ENCRYPTION="uftp.encryption";
	
	/**
	 * enable compression by setting this to "true"
	 */
	public static final String PARAM_ENABLE_COMPRESSION="uftp.compression";

	/**
	 * dummy "file name" used for creating a UFTP session via UNICORE import/export
	 */
	public static final String SESSION_TAG = "__uftp_session__";

	
}
