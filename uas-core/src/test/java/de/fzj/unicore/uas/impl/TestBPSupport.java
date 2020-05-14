package de.fzj.unicore.uas.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.ggf.baseprofile.FinalWSResourceInterfaceDocument;
import org.ggf.baseprofile.ResourcePropertyNamesDocument;
import org.ggf.baseprofile.WSResourceInterfacesDocument;

import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.uas.impl.sms.HomeStorageImpl;

public class TestBPSupport extends TestCase{

	public void test1(){
		UASWSResourceImpl i=new UASWSResourceImpl(){
			@Override
			public QName getResourcePropertyDocumentQName() {
				return null;
			}
		};
		assertTrue(i.getWSResourceInterfaces().toString().contains("QueryResourceProperties"));
		JobManagementImpl j=new JobManagementImpl();
		assertTrue(j.getWSResourceInterfaces().toString().contains("QueryResourceProperties"));
		assertTrue(j.getWSResourceInterfaces().toString().contains("JobManagement"));
		HomeStorageImpl k=new HomeStorageImpl();
		assertTrue(k.getWSResourceInterfaces().toString().contains("QueryResourceProperties"));
		assertTrue(k.getWSResourceInterfaces().toString().contains("StorageManagement"));
		assertFalse(k.getWSResourceInterfaces().toString().contains("JobManagement"));
	}
	
	public void test3()throws Exception{
		Set<QName>qs=new HashSet<QName>();
		QName q1=new QName("foo","bar");
		QName q2=new QName("ham","spam");
		qs.add(q1);
		qs.add(q2);
		ResourcePropertyNamesDocument rd2=BPSupportImpl.getRPNamesProperty(qs);
		
		List<?> l=rd2.getResourcePropertyNames();
		for(Object i: l){
			assertTrue(qs.contains(i));
		}
	}
	
	public void test4()throws Exception{
		QName q1=new QName("foo","bar");
		BPSupportImpl bp=new BPSupportImpl();
		FinalWSResourceInterfaceDocument rd2=bp.getFinalResourceInterfaceRP(q1);
		QName q2=rd2.getFinalWSResourceInterface();
		assertEquals(q2,q1);
	}
	
	public void test5()throws Exception{
		BPSupportImpl bp=new BPSupportImpl();
		bp.addWSResourceInterface(new QName("foo","bar"));
		WSResourceInterfacesDocument d=bp.getWSResourceInterfaces();
		assertTrue(d.toString().contains("bpri1:bar"));
		assertTrue(d.toString().contains("xmlns:bpri1=\"foo\""));
	}
	
	
}
