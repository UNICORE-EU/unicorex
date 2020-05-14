package de.fzj.unicore.uas.security;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.unigrids.services.atomic.types.SecurityType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.TestSecConfigs;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.httpclient.IClientConfiguration;
import junit.framework.Assert;

public class TestSecurityInfo {

	static Kernel kernel;

	@BeforeClass
	public static void setUp()throws Exception{
		FileUtils.deleteQuietly(new File("target","data"));
		UAS uas=new UAS("src/test/resources/secure/uas.config");
		uas.startSynchronous();
		kernel=uas.getKernel();
	}

	@AfterClass
	public static void tearDown()throws Exception{
		kernel.shutdown();
		FileUtils.deleteQuietly(new File("target","data"));
	}

	@Test
	public void testSecurityInfo()throws Exception{
		IClientConfiguration sec = TestSecConfigs.getClientSecurityCfg(true);
		EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.TSF, "default_target_system_factory", kernel);
		TSFClient tsf=new TSFClient(epr, sec);
		Assert.assertNotNull(tsf.getSecurityInfo());
		Assert.assertEquals(2, tsf.getXlogins().length);
		Assert.assertEquals(2, tsf.getXgroups().length);
		Assert.assertEquals(1, tsf.getAcceptedCAs().length);
	}

	@Test
	public void testSecurityPreferencesPersistence()throws Exception{
		IClientConfiguration sec = TestSecConfigs.getClientSecurityCfg(true);
		EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.TSF, "default_target_system_factory", kernel);

		//no preferences - check if the defaults are ok
		TSFClient tsf=new TSFClient(epr, sec);
		TSSClient tss0 = tsf.createTSS();
		SecurityType secInfo0 = tss0.getSecurityInfo();
		Assert.assertEquals("tester", secInfo0.getClientSelectedXlogin());
		Assert.assertEquals("users", secInfo0.getClientSelectedXgroup().getPrimaryGroup());

		//preferences used - check if were applied (here persistence is not yet used - just a normal situation)
		Map<String, String[]> preferences = sec.getETDSettings().getRequestedUserAttributes2();
		preferences.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{"tester_2"});
		tsf=new TSFClient(epr, sec);
		TSSClient tss = tsf.createTSS();
		SecurityType secInfo = tss.getSecurityInfo();
		Assert.assertEquals("tester_2", secInfo.getClientSelectedXlogin());
		Assert.assertEquals("users", secInfo.getClientSelectedXgroup().getPrimaryGroup());

		//access the resource but with different preferences - explicitly set should overwrite any persisted
		Map<String, String[]> preferences2 = sec.getETDSettings().getRequestedUserAttributes2();
		preferences2.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{"tester"});
		preferences2.put(IAttributeSource.ATTRIBUTE_GROUP, new String[]{"audio"});
		tss = new TSSClient(tss.getEPR(), sec);
		SecurityType secInfo2 = tss.getSecurityInfo();
		Assert.assertEquals("tester", secInfo2.getClientSelectedXlogin());
		Assert.assertEquals("audio", secInfo2.getClientSelectedXgroup().getPrimaryGroup());

		//check of persistence - not using any preferences
		Map<String, String[]> preferences3 = sec.getETDSettings().getRequestedUserAttributes2();
		preferences3.remove(IAttributeSource.ATTRIBUTE_XLOGIN);
		preferences3.remove(IAttributeSource.ATTRIBUTE_GROUP);
		tss = new TSSClient(tss.getEPR(), sec);
		SecurityType secInfo3 = tss.getSecurityInfo();
		Assert.assertEquals("tester", secInfo3.getClientSelectedXlogin());
		Assert.assertEquals("users", secInfo3.getClientSelectedXgroup().getPrimaryGroup());


		//and using only one preference
		Map<String, String[]> preferences4 = sec.getETDSettings().getRequestedUserAttributes2();
		preferences4.put(IAttributeSource.ATTRIBUTE_GROUP, new String[]{"audio"});
		tss = new TSSClient(tss.getEPR(), sec);
		SecurityType secInfo4 = tss.getSecurityInfo();
		Assert.assertEquals("tester", secInfo4.getClientSelectedXlogin());
		Assert.assertEquals("audio", secInfo4.getClientSelectedXgroup().getPrimaryGroup());
	}

	@Test
	public void testEPRsContainServerID()throws Exception{
		IClientConfiguration sec = TestSecConfigs.getClientSecurityCfg(true);
		EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.TSF, "default_target_system_factory", kernel);

		//no preferences - check if the defaults are ok
		TSFClient tsf=new TSFClient(epr, sec);
		tsf.createTSS();
		EndpointReferenceType tssEPR=tsf.getAccessibleTargetSystems().get(0);
		String serverDN=WSUtilities.extractServerIDFromEPR(tssEPR);
		System.out.println("Server DN "+serverDN);
		Assert.assertNotNull(serverDN);
	}

}
