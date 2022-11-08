package de.fzj.unicore.xnjs.tsi.remote;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.XNJSSocketFactory;
import de.fzj.unicore.xnjs.util.IOUtils;

/**
 * Small helper to create XNJS server and client sockets for communication with TSI. 
 */
public class TSISocketFactory extends XNJSSocketFactory implements AutoCloseable, Closeable {
	
	private ServerSocket server;

	private final int myPort;

	// this is used to handle the case that we have an SSL config defined, 
	// but still want to DISable SSL for the XNJS/TSI link
	private final boolean disableSSL;
	
	public TSISocketFactory(XNJS xnjs, int myPort)throws Exception{
		super(xnjs);
		this.myPort=myPort;
		TSIProperties tsiProps = xnjs.get(TSIProperties.class);
		disableSSL=tsiProps.getBooleanValue(TSIProperties.TSI_DISABLE_SSL);
		server = init();
	}

	private ServerSocket init()throws Exception{
		if(useSSL()){
			return createSSLServer();
		}
		else{
			return createPlainServer();
		}
	}

	/**
	 * close and re-open the server socket - use wisely
	 */
	public void reInit() throws Exception {
		IOUtils.closeQuietly(server);
		server = init();
	}
	
	private ServerSocket createPlainServer()throws IOException{
		return new ServerSocket(myPort);
	}

	private ServerSocket createSSLServer()throws Exception{
		ServerSocketFactory ssf=getSSLContext().getServerSocketFactory();
		ServerSocket s=ssf.createServerSocket(myPort);
		SSLServerSocket ssl=(SSLServerSocket)s;
		ssl.setNeedClientAuth(security.doSSLAuthn());
		ssl.setEnableSessionCreation(true);
		return s;
	}
	
	public Socket accept()throws IOException{
		Socket s=server.accept();
		if(useSSL()){
			((SSLSocket)s).startHandshake();
		}
		return s;
	}

	public void setSoTimeout(int timeout)throws SocketException{
		server.setSoTimeout(timeout);
	}

	public void close()throws IOException{
		IOUtils.closeQuietly(server);
	}

	public boolean useSSL(){
		return super.useSSL() && !disableSSL;
	}
}