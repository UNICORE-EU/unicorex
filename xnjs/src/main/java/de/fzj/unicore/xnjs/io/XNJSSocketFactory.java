package de.fzj.unicore.xnjs.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import de.fzj.unicore.xnjs.XNJS;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator2;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Creates sockets using the XNJS's security configuration.
 * 
 * @author schuller
 */
public class XNJSSocketFactory extends javax.net.SocketFactory{

	protected final IClientConfiguration security;
	
	private final boolean isSSL;
	
	private SSLContext sslContext=null;

	public XNJSSocketFactory(XNJS xnjs) throws Exception {
		this(xnjs, null);
	}

	public XNJSSocketFactory(XNJS xnjs, Boolean isSSL) throws Exception{
		this.security = xnjs.get(IClientConfiguration.class);
		if (isSSL != null) {
			if (isSSL && !security.isSslEnabled())
				throw new IllegalStateException("Can not enable SSL for XNJS: " +
						"no SSL configuration has been defined.");
			this.isSSL = isSSL;
		} else
			this.isSSL = security.isSslEnabled();
		
	}

	/**
	 * Warning! The returned socket factory will ignore server's hostname to certificate checking setting from the
	 * configuration. Currently this method is used only for SMTP connections, where this is checked in a special
	 * way.
	 */
	public SSLSocketFactory getSSLSocketFactoryForSMTP() throws IOException{
		if(useSSL()){
			return getSSLContext().getSocketFactory();
		}
		else{
			throw new IOException("SSL is not configured!");
		}
	}

	public Socket createSocket(InetAddress source_addr, int port) throws IOException{
		if(useSSL()){
			return getSSLContext().getSocketFactory().createSocket(source_addr, port);
		}
		else{
			return new Socket(source_addr,port);
		}
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException,UnknownHostException {
		if(useSSL()){
			return getSSLContext().getSocketFactory().createSocket(host, port);
		}
		else{
			return new Socket(host,port);
		}
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost,
			int localPort) throws IOException, UnknownHostException {
		if(useSSL()){
			return getSSLContext().getSocketFactory().createSocket(host,port,localHost,localPort);
		}
		else{
			return new Socket(host,port,localHost,localPort);
		}
	}

	@Override
	public Socket createSocket(InetAddress address, int port,
			InetAddress localAddress, int localPort) throws IOException {
		if(useSSL()){
			return getSSLContext().getSocketFactory().createSocket(address,port,localAddress,localPort);
		}
		else{
			return new Socket(address,port,localAddress,localPort);
		}
	}

	public synchronized SSLContext getSSLContext() throws IOException{
		if(sslContext==null){
			sslContext = new SocketFactoryCreator2(security.getCredential(),
					security.getValidator(),
					new HostnameMismatchCallbackImpl(ServerHostnameCheckingMode.WARN))
					.getSSLContext();
		}
		return sslContext;
	}

	public boolean useSSL(){
		return isSSL;
	}
}