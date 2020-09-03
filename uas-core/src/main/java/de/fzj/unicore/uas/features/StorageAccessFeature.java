package de.fzj.unicore.uas.features;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.uas.MetadataManagement;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransfer;
import de.fzj.unicore.uas.fts.FileTransferHomeImpl;
import de.fzj.unicore.uas.fts.uftp.UFTPStartupTask;
import de.fzj.unicore.uas.impl.sms.StorageFactoryHomeImpl;
import de.fzj.unicore.uas.impl.sms.StorageManagementHomeImpl;
import de.fzj.unicore.uas.metadata.MetadataManagementHomeImpl;
import de.fzj.unicore.uas.xtreemfs.XtreemFSStartupTask;
import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import de.fzj.unicore.wsrflite.utils.deployment.FeatureImpl;
import eu.unicore.services.ws.cxf.CXFService;

/**
 * storage access
 * 
 * @author schuller
 */
public class StorageAccessFeature extends FeatureImpl {
	
	public StorageAccessFeature() {
		this.name = "StorageAccess";
	}

	@Override
	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		getInitTasks().add(new UFTPStartupTask(kernel));
		getInitTasks().add(new XtreemFSStartupTask(kernel));	
	}

	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		
		services.add(new Storage(kernel));		
		services.add(new StorageFactory(kernel));		
		services.add(new Metadata(kernel));		
		
		services.add(new ClientFileTransfer(kernel));
		services.add(new ServerServerFTS(kernel));		
		
		return services;
	}
	
	
	public static class Storage extends DeploymentDescriptorImpl {

		public Storage(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public Storage() {
			super();
			this.name = UAS.SMS;
			this.type = CXFService.TYPE;
			this.implementationClass = StorageManagementHomeImpl.class;
			this.interfaceClass = StorageManagement.class;
		}
	}

	public static class Metadata extends DeploymentDescriptorImpl {

		public Metadata(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public Metadata() {
			super();
			this.name = UAS.META;
			this.type = CXFService.TYPE;
			this.implementationClass = MetadataManagementHomeImpl.class;
			this.interfaceClass = MetadataManagement.class;
		}

	}
	
	public static class StorageFactory extends DeploymentDescriptorImpl {

		public StorageFactory(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public StorageFactory() {
			super();
			this.name = UAS.SMF;
			this.type = CXFService.TYPE;
			this.implementationClass = StorageFactoryHomeImpl.class;
			this.interfaceClass = de.fzj.unicore.uas.StorageFactory.class;
		}
	}
	
	public static class ServerServerFTS extends DeploymentDescriptorImpl {

		public ServerServerFTS(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public ServerServerFTS() {
			super();
			this.name = UAS.SERVER_FTS;
			this.type = CXFService.TYPE;
			this.implementationClass = FileTransferHomeImpl.class;
			this.interfaceClass = FileTransfer.class;
		}
	}
	
	public static class ClientFileTransfer extends DeploymentDescriptorImpl {

		public ClientFileTransfer(Kernel kernel){
			super();
			setKernel(kernel);
			this.name = UAS.CLIENT_FTS;
			this.type = CXFService.TYPE;
			this.implementationClass = FileTransferHomeImpl.class;
			this.interfaceClass = FileTransfer.class;
		}
	}

}
