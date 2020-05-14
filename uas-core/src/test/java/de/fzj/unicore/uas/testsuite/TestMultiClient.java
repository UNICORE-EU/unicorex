package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.MultiWSRFClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class TestMultiClient extends AbstractJobRun{

	String url;
	EndpointReferenceType tssepr;

	@FunctionalTest(id="MultiClientTest", 
			description="Tests multicast web service client.")
	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		app.setApplicationVersion("1.0");
		return jdd;
	}
	
	@Override
	protected void beforeStart(JobClient job) {
		System.out.println("Job submitted at: "+job.getSubmissionTime());
	}
	
	@Override
	protected void onFinish(JobClient job) throws Exception{
		
		MultiWSRFClient<BaseUASClient>mc=new MultiWSRFClient<BaseUASClient>();
		mc.addClient(job);
		mc.addClient(tss);
		
		//get RP on all clients in the multi-client
		ResourceProperties rp=mc.makeProxy(ResourceProperties.class);
		GetResourcePropertyDocumentDocument1 in=GetResourcePropertyDocumentDocument1.Factory.newInstance();
		in.addNewGetResourcePropertyDocument();
		rp.GetResourcePropertyDocument(in);
		
		assertTrue(!mc.getErrorsOccurred());
		
		//try round robin
		mc.setMode(MultiWSRFClient.ROUNDROBIN);
		rp=mc.makeProxy(ResourceProperties.class);
		GetResourcePropertyDocument getRP=GetResourcePropertyDocument.Factory.newInstance();
		getRP.setGetResourceProperty(ResourceLifetime.RPterminationTimeQName);
		System.out.println(rp.GetResourceProperty(getRP));
		System.out.println(rp.GetResourceProperty(getRP));
		System.out.println(rp.GetResourceProperty(getRP));
		System.out.println(rp.GetResourceProperty(getRP));
		
		getStdout(job);
		
		
	}

	
}
