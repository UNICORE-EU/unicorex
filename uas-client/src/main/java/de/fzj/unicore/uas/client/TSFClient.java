package de.fzj.unicore.uas.client;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.OperatingSystemType;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument.TerminationTime;
import org.unigrids.x2006.x04.services.tsf.AccessibleTargetSystemReferenceDocument;
import org.unigrids.x2006.x04.services.tsf.CreateTSRDocument;
import org.unigrids.x2006.x04.services.tsf.CreateTSRResponseDocument;
import org.unigrids.x2006.x04.services.tsf.TargetSystemFactoryPropertiesDocument;
import org.unigrids.x2006.x04.services.tsf.TargetSystemFactoryPropertiesDocument.TargetSystemFactoryProperties;
import org.unigrids.x2006.x04.services.tss.AllocationDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.TargetSystemFactory;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Client for the TargetSystemFactory service
 * 
 * @author schuller
 */
public class TSFClient extends BaseUASClient {

	private static final Logger logger=Log.getLogger(Log.CLIENT,TSFClient.class);

	private final TargetSystemFactory tsf;

	public TSFClient(String endpointUrl, EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		super(endpointUrl, epr,sec);
		tsf=makeProxy(TargetSystemFactory.class);
	}

	public TSFClient(EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		this(epr.getAddress().getStringValue(), epr,sec);
	}

	public TSSClient createTSS(CreateTSRDocument in) throws Exception {
		logger.info("Calling target system factory service at: "+getEPR().getAddress().getStringValue());
		CreateTSRResponseDocument res=tsf.CreateTSR(in);
		EndpointReferenceType epr=res.getCreateTSRResponse().getTsrReference();
		return new TSSClient(epr.getAddress().getStringValue(),epr, getSecurityConfiguration());
	}

	/**
	 * create a TSS with the supplied initial termination time
	 * 
	 * @param initialTerminationTime
	 * @throws BaseFault
	 */
	public TSSClient createTSS(Calendar initialTerminationTime) throws Exception {
		CreateTSRDocument in=CreateTSRDocument.Factory.newInstance();
		TerminationTime tt=TerminationTime.Factory.newInstance();
		tt.setCalendarValue(initialTerminationTime);
		in.addNewCreateTSR().setTerminationTime(tt);
		return createTSS(in);
	}

	public TSSClient createTSS() throws Exception {
		CreateTSRDocument in=CreateTSRDocument.Factory.newInstance();
		in.addNewCreateTSR();
		return createTSS(in);
	}

	public TargetSystemFactoryPropertiesDocument getResourcePropertiesDocument()throws Exception{
		return TargetSystemFactoryPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}

	public boolean supportsReservation()throws Exception{
		return getResourcePropertiesDocument().getTargetSystemFactoryProperties().getSupportsReservation();
	}

	public boolean supportsVirtualImages()throws Exception{
		return getResourcePropertiesDocument().getTargetSystemFactoryProperties().getSupportsVirtualImages();
	}

	public OperatingSystemType[] getOperatingSystems() throws Exception{
		return getResourcePropertiesDocument().getTargetSystemFactoryProperties().getOperatingSystemArray();
	}

	/**
	 * get the remaining compute time (typically given in core hours)
	 */
	public List<Allocation> getComputeTimeBudget() throws Exception {
		List<Allocation> budget = new ArrayList<>();
		TargetSystemFactoryProperties p = getResourcePropertiesDocument().getTargetSystemFactoryProperties();
		if(p.isSetComputeTimeBudget()){
			for(AllocationDocument.Allocation alloc: p.getComputeTimeBudget().getAllocationArray()) {
				try{
					budget.add(new Allocation(alloc.getName(), alloc.getRemaining().longValue(),
							alloc.getPercentRemaining().intValue(), alloc.getUnits()));
				}catch(Exception ex) {
					Log.logException("Invalid allocation: "+alloc, ex, logger);
				}
			}
		}
		return budget;
	}
	
	/**
	 * gets the addresses of TSSs that are accessible for this client
	 */
	public List<EndpointReferenceType> getAccessibleTargetSystems() throws Exception {
		List<AccessibleTargetSystemReferenceDocument>tssRefs=
				getResourceProperty(AccessibleTargetSystemReferenceDocument.class);
		List<EndpointReferenceType>res=new ArrayList<EndpointReferenceType>();
		for(AccessibleTargetSystemReferenceDocument x: tssRefs){
			res.add(x.getAccessibleTargetSystemReference());
		}
		return res;
	}
	
	public static class Allocation {
		private final String name;
		private final String units;
		private final long remaining;
		private final int percentRemaining;
		
		public Allocation(String name, long remaining, int percentRemaining, String units){
			this.name = name;
			this.units = units;
			this.percentRemaining = percentRemaining;
			this.remaining = remaining;
		}
		
		public String toString() {
			return "Allocation["+name+" "+remaining+" "+units+" ("+percentRemaining+"%)]";
		}
	}

}
