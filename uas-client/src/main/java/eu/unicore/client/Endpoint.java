package eu.unicore.client;

import java.util.Objects;

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
		ep.setInterfaceName(interfaceName);
		return ep;
	}

	@Override
	public String toString() {
		return url;
	}

	@Override
	public int hashCode() {
		return Objects.hash(interfaceName, serverIdentity, serverPublicKey, url);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Endpoint other = (Endpoint) obj;
		return Objects.equals(interfaceName, other.interfaceName)
				&& Objects.equals(serverIdentity, other.serverIdentity)
				&& Objects.equals(serverPublicKey, other.serverPublicKey) && Objects.equals(url, other.url);
	}

}
