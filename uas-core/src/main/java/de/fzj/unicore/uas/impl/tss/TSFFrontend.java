/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 

package de.fzj.unicore.uas.impl.tss;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyResponseDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.unigrids.services.atomic.types.TextInfoType;
import org.unigrids.x2006.x04.services.tsf.CreateTSRDocument;
import org.unigrids.x2006.x04.services.tsf.CreateTSRResponseDocument;
import org.unigrids.x2006.x04.services.tsf.NameDocument;
import org.unigrids.x2006.x04.services.tsf.SupportsVirtualImagesDocument;
import org.unigrids.x2006.x04.services.tsf.TargetSystemDescriptionType;
import org.unigrids.x2006.x04.services.tsf.TargetSystemFactoryPropertiesDocument;
import org.unigrids.x2006.x04.services.tss.SupportsReservationDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.ResourceReservation;
import de.fzj.unicore.uas.TargetSystem;
import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.tss.rp.AccessibleTSSReferenceRP;
import de.fzj.unicore.uas.impl.tss.rp.ApplicationsResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.AvailableResourcesRP;
import de.fzj.unicore.uas.impl.tss.rp.CPUCountResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.CPUTimeResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.ComputeTimeBudgetRenderer;
import de.fzj.unicore.uas.impl.tss.rp.MemoryPerNodeResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.NodesResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.OperatingSystemResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.PerformanceDataResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.ProcessorResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.TSSReferenceResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.TextInfoResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.TotalCPUsResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.UpSinceResourceProperty;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceNotDestroyedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * Implements the {@link TargetSystemFactory} interface.<br/>
 * 
 * <p>This is a fairly simplistic implementation that does not
 * use all the capabilities of the interface. For all the
 * TargetSystems created by this factory, the same XNJS instance
 * is used. The TargetSystemDescription supplied to CreateTSR() 
 * is ignored.</p>
 * 
 * @author schuller
 */
public class TSFFrontend extends UASBaseFrontEnd implements TargetSystemFactory {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS, TSFFrontend.class);

	private final TargetSystemFactoryImpl tsf;
	
	@Override
	protected void addWSResourceInterfaces(BPSupportImpl baseProfile) {
		super.addWSResourceInterfaces(baseProfile);
		baseProfile.addWSResourceInterface(TSF_PORT);
	}
	

	public TSFFrontend(TargetSystemFactoryImpl tsf){
		super(tsf);
		this.tsf = tsf;
		addRenderer(new UpSinceResourceProperty(tsf));
		addRenderer(new ApplicationsResourceProperty(tsf));
		addRenderer(new TSSReferenceResourceProperty(tsf));
		addRenderer(new AccessibleTSSReferenceRP(tsf));
		addRenderer(new CPUTimeResourceProperty(tsf));
		addRenderer(new NodesResourceProperty(tsf));
		addRenderer(new MemoryPerNodeResourceProperty(tsf));
		addRenderer(new CPUCountResourceProperty(tsf));
		addRenderer(new TotalCPUsResourceProperty(tsf));
		addRenderer(new ProcessorResourceProperty(tsf));
		addRenderer(new ValueRenderer(tsf, RPName) {
			@Override
			protected NameDocument getValue() throws Exception {
				NameDocument nd=NameDocument.Factory.newInstance();
				nd.setName(kernel.getContainerProperties().getValue(ContainerProperties.VSITE_NAME_PROPERTY));
				return nd;
			}
		}); 
		addRenderer(new AvailableResourcesRP(tsf));
		addRenderer(new OperatingSystemResourceProperty(tsf));
		addRenderer(new PerformanceDataResourceProperty(tsf));
		addRenderer(new ValueRenderer(tsf, ResourceReservation.RP_SUPPORTS_RESERVATION) {
			@Override
			protected SupportsReservationDocument getValue() throws Exception {
				SupportsReservationDocument res=SupportsReservationDocument.Factory.newInstance();
				res.setSupportsReservation(tsf.getModel().supportsReservation);
				return res;
			}
		});
		addRenderer(new ValueRenderer(tsf, SupportsVirtualImagesDocument.type.getDocumentElementName()) {
			@Override
			protected SupportsVirtualImagesDocument getValue() throws Exception {
				SupportsVirtualImagesDocument res=SupportsVirtualImagesDocument.Factory.newInstance();
				res.setSupportsVirtualImages(tsf.getModel().supportsVirtualImages);
				return res;
			}
		}); 
		addRenderer(new TextInfoResourceProperty(tsf));
		addRenderer(new ComputeTimeBudgetRenderer(tsf));
	}


	/**
	 * Create a new TargetSystem resource
	 */
	public CreateTSRResponseDocument CreateTSR(CreateTSRDocument in)
			throws BaseFault {
		try {
			Map<String,String>parameters = new HashMap<>();
			Calendar initialTT = null;
			if(in.getCreateTSR().isSetTerminationTime()){
				initialTT=in.getCreateTSR().getTerminationTime().getCalendarValue();
			}
			if(in.getCreateTSR().isSetTargetSystemDescription()){
				TargetSystemDescriptionType tssDesc = in.getCreateTSR().getTargetSystemDescription();
				 for(TextInfoType ti: tssDesc.getTextInfoArray()){
					 parameters.put(ti.getName(),ti.getValue());
				 }
			}
			String tssID = tsf.createTargetSystem(initialTT, parameters);
			String clientName = (tsf.getClient()!=null ? tsf.getClient().getDistinguishedName():"<no client>");
			logger.info("Created new TargetSystem resource <"+tssID+"> for "+clientName);
			CreateTSRResponseDocument response=CreateTSRResponseDocument.Factory.newInstance();
			response.addNewCreateTSRResponse();
			EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.TSS, tssID, TargetSystem.TSS_PORT, true, kernel);
			response.getCreateTSRResponse().setTsrReference(epr);
			return response;
		} catch (Exception e) {
			LogUtil.logException("Did not create new TargetSystem resource.",e,logger);
			throw BaseFault.createFault(e.getMessage());
		}
	}
	
	/**
	 * extract extra parameters from the XML target system description
	 * @param desc
	 */	
	protected Map<String,String>parseInfoFromTSSDesc(TargetSystemDescriptionType desc){
		 Map<String,String> result = new HashMap<String, String>();
		 if(desc!=null){
			 for(TextInfoType ti: desc.getTextInfoArray()){
				 result.put(ti.getName(),ti.getValue());
			 }
		 }
		 return result;
	}

	@Override
	public DestroyResponseDocument Destroy(DestroyDocument in) throws ResourceNotDestroyedFault, ResourceUnknownFault, ResourceUnavailableFault {
		throw ResourceNotDestroyedFault.createFault("Not destroyed."); 
	}

	@Override
	public SetTerminationTimeResponseDocument SetTerminationTime(
			SetTerminationTimeDocument in)
			throws UnableToSetTerminationTimeFault,
			TerminationTimeChangeRejectedFault, ResourceUnknownFault,
			ResourceUnavailableFault {
		throw TerminationTimeChangeRejectedFault.createFault("Not changed.");
	}
		
	@Override
	public QName getResourcePropertyDocumentQName() {
		return TargetSystemFactoryPropertiesDocument.type.getDocumentElementName();
	}
	
	@Override
	public QName getPortType() {
		return TSF_PORT;
	}

}
