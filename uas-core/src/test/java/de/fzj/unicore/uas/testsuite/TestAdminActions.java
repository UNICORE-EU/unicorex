package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.AdminServiceClient;
import de.fzj.unicore.wsrflite.xmlbeans.client.AdminServiceClient.AdminActionResult;

public class TestAdminActions extends Base  {

	protected String url;

	protected TSSClient tss;

	protected JobClient job;

	protected TSFClient tsf;
	
	@Test
	public void testToggleJobSubmission()throws Exception{
		initClients();
		AdminServiceClient cl=getAdminServiceClient();
		assertTrue(cl.getAdminActions().size()>0);
		Map<String,String>param=new HashMap<String, String>();
		String m="Not now, please.";
		param.put("message",m);
		AdminActionResult res=cl.invokeAdminAction("ToggleJobSubmission", param);
		assertTrue(res.successful());
		TargetSystemHomeImpl tssHome=(TargetSystemHomeImpl)kernel.getHome(UAS.TSS);
		assertTrue(!tssHome.isJobSubmissionEnabled());
		assertEquals(m,tssHome.getHighMessage());
		res=cl.invokeAdminAction("ToggleJobSubmission", null);
		assertTrue(tssHome.isJobSubmissionEnabled());
		assertEquals(TargetSystemHomeImpl.DEFAULT_MESSAGE,tssHome.getHighMessage());
	}
	
	private AdminServiceClient getAdminServiceClient()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		EndpointReferenceType e=EndpointReferenceType.Factory.newInstance();
		String u=url+"/AdminService?res=default_admin";
		e.addNewAddress().setStringValue(u);
		return new AdminServiceClient(u,e,kernel.getClientConfiguration());
	}
	
	protected void initClients()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		EndpointReferenceType tsfepr=EndpointReferenceType.Factory.newInstance();
		String tsfUrl=url+"/"+UAS.TSF+"?res="+TargetSystemFactoryHomeImpl.DEFAULT_TSF;
		tsfepr.addNewAddress().setStringValue(tsfUrl);
		tsf=new TSFClient(tsfUrl,tsfepr,kernel.getClientConfiguration());
		tss=tsf.createTSS();
		Calendar c=tss.getCurrentTime();
		assertTrue(c!=null);
	}
}
