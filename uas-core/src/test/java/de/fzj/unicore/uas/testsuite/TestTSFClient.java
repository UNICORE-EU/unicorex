package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.unigrids.services.atomic.types.AvailableResourceType;
import org.unigrids.services.atomic.types.AvailableResourceTypeType;
import org.unigrids.services.atomic.types.SecurityDocument;
import org.unigrids.x2006.x04.services.tsf.PerformanceDataDocument.PerformanceData;
import org.unigrids.x2006.x04.services.tsf.TargetSystemFactoryPropertiesDocument;
import org.unigrids.x2006.x04.services.tsf.TargetSystemFactoryPropertiesDocument.TargetSystemFactoryProperties;
import org.unigrids.x2006.x04.services.tss.ApplicationResourceType;
import org.unigrids.x2006.x04.services.tss.JobReferenceDocument;

import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.EnumerationClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class TestTSFClient extends RunDate {

	@FunctionalTest(id="TSFTest", 
					description="Tests TargetSystemFactory service")
	@Override
	public void testRunJob()throws Exception{
		initClients();
		//assert we have the EPR of the new TSS in the TSF properties
		String s=tsf.getResourceProperty(TargetSystemFactory.RPTSSReferences);
		assertTrue(s.contains(tss.getEPR().getAddress().getStringValue()));
		long start=System.currentTimeMillis();
		int n=tsf.getAccessibleTargetSystems().size();
		System.out.println("Have "+n+" accessible TSS, took: "+(System.currentTimeMillis()-start)+ " ms.");
		assertTrue(n==1);
		
		testRP(tss);

		EnumerationClient<JobReferenceDocument> c=tss.getJobReferenceEnumeration();
		assertTrue(c!=null);
		c.setUpdateInterval(-1);
		runJob(tss);
		System.out.println("Enumeration : "+c.getUrl());
		System.out.println(c.getResourcePropertyDocument());
		assertTrue(c.getNumberOfResults()==1);
		System.out.println(c.getResults(0, 1).get(0));
		tss.destroy();

		//check we get a resource unknown fault
		try{
			tss.getCurrentTime();
		}
		catch(Exception ex){
			System.out.println("OK, got "+LogUtil.createFaultMessage("", ex));
		}

		//assert the TSS ref has gone from the list
		s=tsf.getResourceProperty(TargetSystemFactory.RPTSSReferences);
		assertTrue(!s.contains(tss.getEPR().getAddress().getStringValue()));

		tsf.createTSS();
		tsf.createTSS();
		tsf.createTSS();
		//check we have three refs now
		TargetSystemFactoryProperties rp=(TargetSystemFactoryProperties)
		(tsf.GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse()
				.selectChildren(TargetSystemFactoryPropertiesDocument.type.getDocumentElementName())[0]);
		assertTrue(rp.getTargetSystemReferenceArray().length==3);

		testRP(tsf);

		//check version
		String v=tsf.getServerVersion();
		assertTrue(UAS.getVersion(UAS.class).equals(v));
	}

	@Override
	protected void onFinish(JobClient job) throws Exception{
		//do not cleanup the job
	}

	//checks the TSS resource properties
	private void testRP(TSSClient tss)throws Exception{
		
		//assert we have text info properties
		assertTrue(tss.getTextInfo().size()==2);

		//assert that we have the addon storage mapped to /tmp
		assertTrue(tss.getStorages().size()==1);
		StorageClient sms2=tss.getStorage("TEMP");
		assertTrue("TEMP".equals(sms2.getStorageName()));
		
		//check the OS property
		String osDescription=tss.getOperatingSystemInfo();
		System.out.println("OS: "+osDescription);
		assertTrue(osDescription.startsWith("LINUX"));

		//check applications resourceproperties
		assertTrue(tss.getApplications().size()>0);
		for(ApplicationResourceType app: tss.getApplications()){
			System.out.println("APP: "+app.getApplicationName());
		}

		//check custom resource "Queue"
		AvailableResourceType[]sr=tss.getResourcePropertiesDocument().getTargetSystemProperties().getAvailableResourceArray();
		assertTrue(sr.length>0);
		AvailableResourceType queueSSR=getAvailableResource("Queue", sr);
		assertNotNull(queueSSR);
		assertTrue(queueSSR.getName().equals("Queue"));
		assertTrue(queueSSR.getType().equals(AvailableResourceTypeType.CHOICE));
		assertTrue(queueSSR.getDefault().equals("normal"));
		assertTrue(3==queueSSR.getAllowedValueArray().length);

		//check custom boolean resource
		AvailableResourceType booleanSSR=getAvailableResource("PriviledgedGoldCustomerExecution", sr);
		assertNotNull(booleanSSR);
		System.out.println(booleanSSR);
		
		//security info
		String sd=tss.getResourceProperty(SecurityDocument.type.getDocumentElementName());
		SecurityDocument sDoc=SecurityDocument.Factory.parse(sd);
		System.out.println(sDoc);
		//xlogin/xgroup (even if empty in this case)
		System.out.println("Xlogins "+Arrays.asList(tss.getXlogins()));
		System.out.println("Xgroups "+Arrays.asList(tss.getXgroups()));
	}

	public AvailableResourceType getAvailableResource(String name, AvailableResourceType[]sr){
		for(AvailableResourceType a: sr){
			if(name.equals(a.getName()))return a;
		}
		return null;
	}
	
	//checks on the TSF resource properties
	private void testRP(TSFClient tsf)throws Exception{
		TargetSystemFactoryProperties rp=tsf.getResourcePropertiesDocument().getTargetSystemFactoryProperties();
		AvailableResourceType[] sr=rp.getAvailableResourceArray();
		assertTrue(sr.length>0);

		//check performance data RP
		PerformanceData[]pd=rp.getPerformanceDataArray();
		assertTrue(pd!=null);
		assertTrue(pd.length>0);
		System.out.println(pd[0]);
	
		//server version
		System.out.println("TSF version: "+rp.getVersion());
		
		// compute budget
		System.out.println("TSF version: "+rp.getComputeTimeBudget());
		assertTrue(rp.getComputeTimeBudget().getAllocationArray().length==2);
	}

}
