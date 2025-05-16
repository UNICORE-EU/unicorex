package eu.unicore.client;

/**
 * A REST service endpoint including some metadata about it
 * 
 * @author schuller
 */
public class Endpoint {

	private final String url;
	
	private String serverIdentity;
	
	private String serverPublicKey;
	
	private String interfaceName;
	
	public Endpoint(String url) {
		this.url = url;
	}

	public String getServerIdentity() {
		return serverIdentity;
	}

	public void setServerIdentity(String serverIdentity) {
		this.serverIdentity = serverIdentity;
	}

	public String getServerPublicKey() {
		return serverPublicKey;
	}

	public void setServerPublicKey(String serverPublicKey) {
		this.serverPublicKey = serverPublicKey;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getUrl() {
		return url;
	}

	public Endpoint cloneTo(String url){
		Endpoint ep = new Endpoint(url);
		ep.setServerIdentity(serverIdentity);
		ep.setServerPublicKey(serverPublicKey);
		return ep;
	}

	@Override
	public String toString() {
		return url;
	}
}
