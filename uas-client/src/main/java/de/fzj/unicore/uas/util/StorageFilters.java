package de.fzj.unicore.uas.util;

import org.unigrids.services.atomic.types.ProtocolType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.lookup.AddressFilter;

/**
 * some useful filters for storages
 *
 * @author schuller
 */
public class StorageFilters {
	
	private StorageFilters(){}
	
	public static class ByName implements AddressFilter<StorageClient>{

		private final String name;
		
		public ByName(String name){
			this.name=name;
		}
		
		@Override
		public boolean accept(EndpointReferenceType epr) {
			return true;
		}

		@Override
		public boolean accept(String uri) {
			return true;
		}

		@Override
		public boolean accept(StorageClient client) throws Exception {
			return name==null || name.equals(client.getStorageName());
		}
		
	}
	

	public static class SupportingProtocol implements AddressFilter<StorageClient>{

		private final ProtocolType.Enum[] protocols;
		
		public SupportingProtocol(ProtocolType.Enum... protocols){
			this.protocols=protocols;
		}
		
		@Override
		public boolean accept(EndpointReferenceType epr) {
			return true;
		}

		@Override
		public boolean accept(String uri) {
			return true;
		}

		@Override
		public boolean accept(StorageClient client) throws Exception {
			return protocols==null || protocols.length==0 || 
					client.findSupportedProtocol(protocols)!=null ;
		}
		
	}
}
