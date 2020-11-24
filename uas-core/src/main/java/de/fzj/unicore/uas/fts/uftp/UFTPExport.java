package de.fzj.unicore.uas.fts.uftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.sms.ImportFileDocument;
import org.unigrids.x2006.x04.services.sms.ImportFileResponseDocument;

import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.UFTPConstants;
import de.fzj.unicore.uas.client.UFTPFileTransferClient;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.uas.xnjs.U6FileExportBase;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import de.fzj.unicore.xnjs.util.AsyncCommandHelper;
import de.fzj.unicore.xnjs.util.ResultHolder;
import eu.unicore.uftp.client.UFTPClient;
import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.dpc.AuthorizationFailureException;
import eu.unicore.uftp.server.workers.UFTPWorker;
import eu.unicore.util.Log;

public class UFTPExport extends U6FileExportBase implements UFTPConstants {

	private String secret;

	/**
	 * whether the Java UFTP library should be used directly, which requires
	 * that the UNICORE/X has access to the file system<br/>
	 * If set to <code>false</code>, the TSI is used to run the UFTP client
	 */
	private final boolean localMode;

	// host address of the host where the UFTP client code runs
	// this is either (in local mode) the UNICORE/X host, or one of the TSI
	// nodes
	private final String clientHost;

	// client host address as sent to the UFTPD server
	// in general this will be the same as "clientHost", but
	// in some cases it can be required that they differ
	private final String clientIPAddresses;

	// can be overriden by client
	private boolean useEncryption = false;
	private int streams = 1;
	private boolean disableSessionMode = false;

	private final UFTPProperties uftpProperties;

	private boolean sessionMode = false;

	private StringBuilder fileList = new StringBuilder();

	public UFTPExport(XNJS config) {
		super(config);
		uftpProperties = kernel.getAttribute(UFTPProperties.class);
		localMode = uftpProperties.getBooleanValue(UFTPProperties.PARAM_CLIENT_LOCAL);
		info.setProtocol(ProtocolType.UFTP.toString());
		clientHost = setupClientHost();
		String ip = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_IP);
		clientIPAddresses = ip!=null? ip : clientHost;
	}

	@Override
	protected UFTPFileTransferClient getFTClient() throws Exception {
		String url = fileTransferInstanceEpr.getAddress().getStringValue();
		UFTPFileTransferClient c = new UFTPFileTransferClient(url, fileTransferInstanceEpr, sec);
		c.setSecret(secret);
		return c;
	}

	@Override
	public void setExtraParameters(Map<String, String> params) {
		for (Map.Entry<String, String> e : params.entrySet()) {
			String key = e.getKey();
			String value = e.getValue();
			if (UFTPFileTransferClient.PARAM_ENABLE_ENCRYPTION.equals(key)) {
				useEncryption = Boolean.parseBoolean(value);
			}
			if (UFTPFileTransferClient.PARAM_STREAMS.equals(key)) {
				streams = Integer.parseInt(value);
			}
			if("uftp.disableSessionMode".equals(key)){
				disableSessionMode = Boolean.parseBoolean(value);
			}
		}
	}

	@Override
	protected Map<String, String> getExtraParameters() {
		Map<String, String> result = new HashMap<String, String>();
		result.put(UFTPFileTransferClient.PARAM_CLIENT_HOST, clientIPAddresses);
		result.put(UFTPFileTransferClient.PARAM_STREAMS, String.valueOf(getNumberOfStreams()));
		secret = UUID.randomUUID().toString();
		result.put(UFTPFileTransferClient.PARAM_SECRET, secret);
		result.put(PARAM_ENABLE_ENCRYPTION, String.valueOf(useEncryption));
		return result;
	}

	/**
	 * optimized implementation
	 */
	@Override
	protected void runTransfers() throws Exception {

		boolean noSessionMode = disableSessionMode || checkPossibilityForLocalCopy() || filesToTransfer.size() == 1;

		if (noSessionMode) {
			super.runTransfers();
			return;
		}
		sessionMode = true;
		setupSessionMode(sms);
		super.runTransfers();
		finishSessionMode();
	}

	@Override
	protected void transferFileFromRemote(Pair<String, Long> sourceDesc, String target) throws Exception {
		if (sessionMode) {
			sessionModeTransferFile(sourceDesc, target);
		} else {
			super.transferFileFromRemote(sourceDesc, target);
		}
	}

	/**
	 * in session mode, there are two ways to transfer a file if local mode, the
	 * file is transferred immediately using the session client, else, the
	 * remote file name and local target are written to the command file
	 */
	protected void sessionModeTransferFile(Pair<String, Long> sourceDesc, String currentTarget) throws Exception {
		String currentSource = sourceDesc.getM1();
		// chop off leading "/" since UFTPD will otherwise treat it as an
		// absolute filename
		while (currentTarget.startsWith("/"))
			currentTarget = currentTarget.substring(1);
		if (localMode) {
			long size = sourceDesc.getM2();
			InputStream is = getStorageAdapter().getInputStream(currentSource);
			try {
				getSessionClient().put(currentTarget, size, is);
			} finally {
				is.close();
			}
		} else {
			fileList.append("PUT ").append("\"").append(currentSource).append("\" ").append("\"").append(currentTarget)
			.append("\"").append("\n");
		}
	}

	protected void setupSessionMode(StorageClient sms) throws Exception {
		ImportFileDocument req = ImportFileDocument.Factory.newInstance();
		req.addNewImportFile().setDestination(UFTPWorker.sessionModeTag);
		req.getImportFile().setProtocol(ProtocolType.UFTP);
		Map<String, String> ep = getExtraParameters();
		if (ep != null && ep.size() > 0) {
			req.getImportFile().setExtraParameters(convert(ep));
		}
		try {
			ImportFileResponseDocument res = sms.ImportFile(req);
			fileTransferInstanceEpr = res.getImportFileResponse().getImportEPR();
		} catch (Exception ex) {
			// TODO can we handle an old UFTP server here?
		}
		ftc = getFTClient();
	}

	protected void finishSessionMode() throws Exception {
		if (localMode) {
			getSessionClient().close();
		} else {
			String cmdFile = ".uftp-" + info.getUniqueId();
			OutputStream os = getStorageAdapter().getOutputStream(cmdFile);
			try {
				IOUtils.write(fileList.toString(), os, "UTF-8");
			} finally {
				os.close();
			}
			runAsync(cmdFile);
		}
	}

	private UFTPSessionClient sessionClient;

	private UFTPSessionClient getSessionClient() throws AuthorizationFailureException, IOException {
		if (sessionClient == null) {
			UFTPFileTransferClient uftc = (UFTPFileTransferClient) ftc;
			sessionClient = new UFTPSessionClient(uftc.getServerHosts(), uftc.getServerPort());
			sessionClient.setSecret(secret);
			sessionClient.setNumConnections(getNumberOfStreams());
			sessionClient.connect();
		}
		return sessionClient;
	}

	@Override
	protected void doRun(String localFile) throws Exception {
		checkReadPermission(localFile);
		if (localMode) {
			super.doRun(localFile);
		} else {
			runAsync(localFile);
		}
	}

	protected void checkReadPermission(String localFile) throws Exception {
		InputStream is = getInputStream(localFile);
		try {
			is.read();
		} finally {
			if (is != null)
				is.close();
		}
	}

	private String setupClientHost(){
		String clientHost=uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_HOST);
		if(clientHost==null){
			if(localMode){
				clientHost = getLocalHost();
			}
			else{
				// select one of the configured TSI nodes
				TSIConnectionFactory tcf = configuration.get(TSIConnectionFactory.class);
				if(tcf == null){
					clientHost = getLocalHost();
				}
				else{
					String[] tsis = tcf.getTSIHosts();
					clientHost = tsis[new Random().nextInt(tsis.length)];
				}
			}
		}
		return clientHost;
	}

	private String getLocalHost(){
		try{
			return InetAddress.getLocalHost().getCanonicalHostName();
		}catch(Exception ex){
			return "localhost";
		}
	}

	protected int getNumberOfStreams() {
		return streams;
	}

	private AsyncCommandHelper ach;

	protected void runAsync(String localFile) throws Exception {
		String cmd = getCommandLine(localFile);
		ach = new AsyncCommandHelper(configuration, cmd, info.getUniqueId(), info.getParentActionID(), client);
		ach.setPreferredExecutionHost(clientHost);
		ach.submit();
		int c = 0;
		int interval = 1000;
		while (!ach.isDone()) {
			try {
				checkCancelled();
			} catch (CancelledException ce) {
				try {
					ach.abort();
				} catch (Exception e) {
					logger.warn("Could not abort UFTP client run.");
				}
				throw ce;
			}
			c++;
			if(c % 3 == 0){
				try{
					long transferredBytes = ftc.getTransferredBytes();
					info.setTransferredBytes(transferredBytes);
				}catch(Exception e){}
			}
			
			Thread.sleep(interval);
		}
		ResultHolder res = ach.getResult();
		if (res.getExitCode() == null || res.getExitCode() != 0) {
			String message = "UFTP data upload failed with exit code <" + res.getExitCode() + ">.";
			try {
				String error = res.getStdErr();
				if (error != null)
					message += " Error details: " + error;
			} catch (IOException ex) {
				Log.logException("Could not read UFTP stderr", ex, logger);
			}
			throw new Exception(message);
		}
	}

	private String getCommandLine(String localFile) throws Exception {
		return sessionMode ? getSessionModeCommandLine(localFile) : getSingleFileCommandLine(localFile);
	}

	private String getSingleFileCommandLine(String localFile) throws Exception {
		String uftp = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_EXECUTABLE);
		UFTPFileTransferClient client = getFTClient();
		String host = client.asString(client.getServerHosts());
		int port = client.getServerPort();
		int streams = client.getStreams();
		String key = client.getEncryptionKey();
		boolean compress = client.isCompressionEnabled();
		String file = workdir + "/" + localFile;
		int buf = uftpProperties.getIntValue(UFTPProperties.PARAM_BUFFERSIZE);
		return uftp + " "
		+ UFTPClient.makeCommandline(host, port, file, true, secret, streams, key, false, compress, buf);
	}

	private String getSessionModeCommandLine(String cmdFile) throws Exception {
		String uftp = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_EXECUTABLE);
		UFTPFileTransferClient client = getFTClient();
		String host = client.asString(client.getServerHosts());
		int port = client.getServerPort();
		int streams = client.getStreams();
		String key = client.getEncryptionKey();
		boolean compress = client.isCompressionEnabled();
		int buf = uftpProperties.getIntValue(UFTPProperties.PARAM_BUFFERSIZE);
		return uftp + " -S "
		+ UFTPSessionClient.makeCommandline(host, port, workdir, secret, streams, key, buf, compress, cmdFile);
	}
}
