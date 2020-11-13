package de.fzj.unicore.uas.fts.uftp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.ExternalSystemConnector;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.TimeoutRunner;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;
import eu.unicore.uftp.server.requests.UFTPBaseRequest;
import eu.unicore.uftp.server.requests.UFTPPingRequest;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * handle communications to the UFTPD control socket
 *
 * @author schuller
 */
public class UFTPConnector implements ExternalSystemConnector {

	private static final Logger logger=Log.getLogger(Log.SERVICES, UFTPConnector.class);

	private final Kernel kernel; 

	private final UFTPProperties cfg;

	private Status status=Status.UNKNOWN;
	private String statusMessage;
	private long lastChecked;

	public UFTPConnector(Kernel kernel, UFTPProperties properties){
		this.kernel = kernel;
		this.cfg = properties;
	}

	public boolean isUFTPEnabled(){
		return cfg.getValue(UFTPProperties.PARAM_SERVER_HOST)!=null;
	}

	@Override
	public String getExternalSystemName() {
		return "UFTP Server";
	}

	@Override
	public String getConnectionStatusMessage(){
		checkConnection();	
		return statusMessage;
	}

	@Override
	public Status getConnectionStatus(){
		checkConnection();
		return status;
	}

	private void checkConnection(){
		if (lastChecked+60000>System.currentTimeMillis())
			return;

		if(!isUFTPEnabled()){
			status = Status.NOT_APPLICABLE;
			statusMessage = "UFTP is not enabled";
			return;
		}
		try{
			doSendRequest(new UFTPPingRequest());
		}catch(Exception e){/*handled already*/}
	}

	/**
	 * send request via UFTPD control channel
	 * 
	 * @return reply from uftpd
	 * @throws IOException in case of IO errors or timeout
	 */
	public String sendRequest(final UFTPBaseRequest job)throws IOException{
		if(Status.OK!=getConnectionStatus()){
			throw new IOException(statusMessage);
		}
		return doSendRequest(job);
	}

	private String doSendRequest(final UFTPBaseRequest job)throws IOException{

		final String controlHost=cfg.getValue(UFTPProperties.PARAM_COMMAND_HOST);
		final int controlPort=cfg.getIntValue(UFTPProperties.PARAM_COMMAND_PORT);
		final boolean disableSSL=cfg.getBooleanValue(UFTPProperties.PARAM_COMMAND_SSL_DISABLE);
		final int timeout=cfg.getIntValue(UFTPProperties.PARAM_COMMAND_TIMEOUT);

		Callable<String>task=new Callable<String>(){
			@Override
			public String call() throws Exception {
				Socket socket=null;
				if(disableSSL){
					socket=new Socket(InetAddress.getByName(controlHost),controlPort);
				}
				else{
					IClientConfiguration cc=kernel.getClientConfiguration();
					SSLSocketFactory f=SocketFactoryCreator.getSocketFactory(cc.getCredential(), cc.getValidator(), getRandom());
					socket=f.createSocket(controlHost, controlPort);
				}
				if(logger.isDebugEnabled()){
					logger.debug("Sending "+job.getClass().getSimpleName()+" request to "
							+controlHost+":"+controlPort+", SSL="+!disableSSL);
				}
				try {
					return job.sendTo(socket);
				} finally {
					IOUtils.closeQuietly(socket);
				}
			}
		};
		TimeoutRunner<String>tr=new TimeoutRunner<String>(task, 
				kernel.getContainerProperties().getThreadingServices(), 
				timeout, TimeUnit.SECONDS);
		
		String res = null;
		try{
			res = tr.call();
		}
		catch(Exception e){
			String err = Log.createFaultMessage("Error", e);
			status=Status.DOWN;
			statusMessage="CAN'T CONNECT TO UFTPD "+controlHost+":"+controlPort+" ["+err+"]";
			throw new IOException(e);
		}
		finally{
			lastChecked=System.currentTimeMillis();
		}
		status=Status.OK;
		statusMessage="OK [connected to UFTPD "+controlHost+":"+controlPort+"]";
		return res;
	}

	private static SecureRandom random=null;

	private synchronized SecureRandom getRandom(){
		if(random==null){
			random=new SecureRandom();
		}
		return random;
	}
}
