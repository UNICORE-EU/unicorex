package de.fzj.unicore.uas;

import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.HttpFileTransferClient;
import de.fzj.unicore.uas.client.UFTPFileTransferClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.services.ClientCapabilities;
import eu.unicore.services.ClientCapability;

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
				
				SOAP_BFT_Client,
				SOAP_UFTP_Client,
				
		};
	}

	/**
	 * SOAP/XML file transfer capability
	 */
	public static interface FTClientCapability extends ClientCapability{
		
		public String getProtocol();
		
		public Class<? extends FileTransferClient>getImplementation();
	}
	
	
	private static FTClientCapability SOAP_BFT_Client = new FTClientCapability(){
		public String getProtocol() {
			return "BFT";
		}
		public Class<? extends FileTransferClient> getImplementation() {
			return HttpFileTransferClient.class;
		}
		public Class<?> getInterface() {
			return FileTransferClient.class;
		}
	};
	
	private static ClientCapability SOAP_UFTP_Client=new FTClientCapability(){

		@Override
		public Class<? extends FileTransferClient> getImplementation() {
			return UFTPFileTransferClient.class;
		}

		@Override
		public String getProtocol() {
			return "UFTP";
		}

		@Override
		public Class<?> getInterface() {
			return FileTransferClient.class;
		}

	}; 
	

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
