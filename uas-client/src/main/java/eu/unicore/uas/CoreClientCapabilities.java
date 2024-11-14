package eu.unicore.uas;

import eu.unicore.client.data.FiletransferClient;
import eu.unicore.services.restclient.ClientCapabilities;
import eu.unicore.services.restclient.ClientCapability;

/**
 * advertises client capabilities of the uas-client module
 *
 * @author schuller
 */
public class CoreClientCapabilities implements ClientCapabilities {

	@Override
	public ClientCapability[] getClientCapabilities() {
		return new ClientCapability[]{
				REST_BFT_Client,
				REST_UFTP_Client,
		};
	}	

	/**
	 * REST file transfer capability
	 */
	public static interface RESTFTClientCapability extends ClientCapability{
		
		public String getProtocol();
		
		public Class<? extends FiletransferClient>getImplementation();
	}
	
	private static RESTFTClientCapability REST_BFT_Client = new RESTFTClientCapability(){
		public String getProtocol() {
			return "BFT";
		}
		public Class<? extends FiletransferClient> getImplementation() {
			return eu.unicore.client.data.HttpFileTransferClient.class;
		}
		public Class<?> getInterface() {
			return FiletransferClient.class;
		}
	};
	
	private static RESTFTClientCapability REST_UFTP_Client = new RESTFTClientCapability(){
		public String getProtocol() {
			return "UFTP";
		}
		public Class<? extends FiletransferClient> getImplementation() {
			return eu.unicore.client.data.UFTPFileTransferClient.class;
		}
		public Class<?> getInterface() {
			return FiletransferClient.class;
		}
	};
}
