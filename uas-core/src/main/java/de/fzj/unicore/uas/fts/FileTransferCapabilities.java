package de.fzj.unicore.uas.fts;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.uas.fts.http.HttpFileTransferImpl;
import de.fzj.unicore.uas.fts.uftp.LogicalUFTPServer;
import de.fzj.unicore.uas.fts.uftp.RESTUFTPExport;
import de.fzj.unicore.uas.fts.uftp.RESTUFTPImport;
import de.fzj.unicore.uas.fts.uftp.UFTPFileTransferImpl;
import de.fzj.unicore.uas.xnjs.RESTFileExportBase;
import de.fzj.unicore.uas.xnjs.RESTFileImportBase;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.io.IFileTransfer;

/**
 * Describes the available filetransfers in UAS Core<br/>
 * 
 * Additionally there are some utility methods for finding the available filetransfer
 * capabilities
 * 
 * @author schuller
 */
public class FileTransferCapabilities {

	/**
	 * get the appropriate FT implementation for the given protocol 
	 * by checking the available {@link FileTransferCapabilities}
	 * 
	 * @param protocol - the protocol name
	 * @return a file transfer implementation
	 */
	public static FileTransferImpl getFileTransferImpl(String protocol, Kernel kernel) throws Exception {
		FileTransferImpl impl = null;
		for(U6FileTransferCapability f: kernel.getCapabilities(U6FileTransferCapability.class)){
			if(f.getProtocol().equals(protocol)){
				String clazz=f.getImplementation().getName();
				impl = (FileTransferImpl)Class.forName(clazz).getConstructor().newInstance();
				break;
			}
		}
		return impl;
	}

	public static List<String> getProtocols(Kernel kernel) {
		List<String> protocols = new ArrayList<>();
		for(U6FileTransferCapability f: kernel.getCapabilities(U6FileTransferCapability.class)){
			protocols.add(f.getProtocol());
		}
		return protocols;
	}

	public static FileTransferCapability getCapability(String protocol, Kernel kernel) {
		FileTransferCapability capability = null;
		for(U6FileTransferCapability f: kernel.getCapabilities(U6FileTransferCapability.class)){
			if(f.getProtocol().equals(protocol)){
				capability = f;
				break;
			}
		}
		return capability;
	}

	public static abstract class U6FileTransferCapability implements FileTransferCapability, KernelInjectable {

		protected Kernel kernel;

		public void setKernel(Kernel kernel){
			this.kernel = kernel;
		}

	}


	public static FileTransferCapability REST_BASE = new FileTransferCapabilities.U6FileTransferCapability() {

		@Override
		public String getProtocol() {
			return "BFT";
		}

		@Override
		public Class<?> getImplementation() {
			return HttpFileTransferImpl.class;
		}

		@Override
		public Class<?> getInterface() {
			return FileTransferImpl.class;
		}

		@Override
		public Class<? extends IFileTransfer> getExporter() {
			return RESTFileExportBase.class;
		}

		@Override
		public Class<? extends IFileTransfer> getImporter() {
			return RESTFileImportBase.class;
		}

		@Override
		public Class<? extends IFTSController> getFTSImportsController(){
			return ImportsController.class;
		}

		@Override
		public Class<? extends IFTSController> getFTSExportsController(){
			return ExportsController.class;
		}

		@Override
		public boolean isAvailable(){
			return true;
		}
	};
	
	public static FileTransferCapability REST_UFTP = new FileTransferCapabilities.U6FileTransferCapability() {

		@Override
		public String getProtocol() {
			return "UFTP";
		}

		@Override
		public Class<?> getImplementation() {
			return UFTPFileTransferImpl.class;
		}

		@Override
		public Class<?> getInterface() {
			return FileTransferImpl.class;
		}

		@Override
		public Class<? extends IFileTransfer> getExporter() {
			return RESTUFTPExport.class;
		}

		@Override
		public Class<? extends IFileTransfer> getImporter() {
			return RESTUFTPImport.class;
		}

		@Override
		public Class<? extends IFTSController> getFTSImportsController(){
			return ImportsController.class;
		}

		@Override
		public Class<? extends IFTSController> getFTSExportsController(){
			return ExportsController.class;
		}

		@Override
		public boolean isAvailable(){
			LogicalUFTPServer c = kernel.getAttribute(LogicalUFTPServer.class);
			return c!=null && c.isUFTPAvailable();
		}
	};

}
