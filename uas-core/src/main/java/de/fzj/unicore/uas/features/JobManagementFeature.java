package de.fzj.unicore.uas.features;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.uas.JobManagement;
import de.fzj.unicore.uas.ReservationManagement;
import de.fzj.unicore.uas.TargetSystem;
import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.job.JobManagementHomeImpl;
import de.fzj.unicore.uas.impl.job.ws.JobFrontend;
import de.fzj.unicore.uas.impl.reservation.ReservationManagementHomeImpl;
import de.fzj.unicore.uas.impl.reservation.ws.ReservationFrontend;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import de.fzj.unicore.uas.impl.tss.rp.TSFFrontend;
import de.fzj.unicore.uas.impl.tss.rp.TSSFrontend;
import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import de.fzj.unicore.wsrflite.utils.deployment.FeatureImpl;
import eu.unicore.services.ws.cxf.CXFService;

/**
 * job execution
 * 
 * @author schuller
 */
public class JobManagementFeature extends FeatureImpl {

	public JobManagementFeature() {
		this.name = "JobManagement";
	}

	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		
		services.add(new TSF(kernel));		
		services.add(new TSS(kernel));		
		services.add(new JMS(kernel));
		services.add(new Reservation(kernel));		
		services.add(new StorageAccessFeature.Storage(kernel));		
		services.add(new StorageAccessFeature.ServerServerFTS(kernel));		
		services.add(new StorageAccessFeature.ClientFileTransfer(kernel));
		return services;
	}
	
	
	public static class TSF extends DeploymentDescriptorImpl {

		public TSF(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public TSF() {
			super();
			this.name = UAS.TSF;
			this.type = CXFService.TYPE;
			this.implementationClass = TargetSystemFactoryHomeImpl.class;
			this.interfaceClass = TargetSystemFactory.class;
			this.frontendClass = TSFFrontend.class;
		}
	}

	public static class TSS extends DeploymentDescriptorImpl {

		public TSS(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public TSS() {
			super();
			this.name = UAS.TSS;
			this.type = CXFService.TYPE;
			this.implementationClass = TargetSystemHomeImpl.class;
			this.interfaceClass = TargetSystem.class;
			this.frontendClass = TSSFrontend.class;
		}

	}
	
	public static class JMS extends DeploymentDescriptorImpl {

		public JMS(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public JMS() {
			super();
			this.name = UAS.JMS;
			this.type = CXFService.TYPE;
			this.implementationClass = JobManagementHomeImpl.class;
			this.interfaceClass = JobManagement.class;
			this.frontendClass = JobFrontend.class;
		}
	}
	
	public static class Reservation extends DeploymentDescriptorImpl {

		public Reservation(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public Reservation() {
			super();
			this.name = UAS.RESERVATIONS;
			this.type = CXFService.TYPE;
			this.implementationClass = ReservationManagementHomeImpl.class;
			this.interfaceClass = ReservationManagement.class;
			this.frontendClass = ReservationFrontend.class;
		}
	}
}
