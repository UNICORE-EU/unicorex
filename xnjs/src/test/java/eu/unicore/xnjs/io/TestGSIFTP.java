package eu.unicore.xnjs.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.Xlogin;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.impl.GSIFTPDownload;
import eu.unicore.xnjs.io.impl.GSIFTPUpload;
import eu.unicore.xnjs.tsi.TSI;

public class TestGSIFTP extends EMSTestBase {

	@Test
	public void testGSIDownload()throws Exception{
		xnjs.setProperty("XNJS.localtsi.useShell", "false");
		
		String proxy="proxies are bad for you";
		String dummyParent=createDummyParent();
		String wd=getWorkingDir(dummyParent);
		Client client=createClient(proxy);
		
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY, "/bin/echo");
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY_PARAMS, "args");
		GSIFTPDownload d=new GSIFTPDownload(client,wd,new URI("http://test"),"test",xnjs);
		d.getInfo().setParentActionID(dummyParent);
		String cmd=d.makeCommandline();
		System.out.println(cmd);
		assertEquals("/bin/echo args 'http://test' 'file:"+getWorkingDir(dummyParent)+"test'",cmd);
		assertEquals(Status.CREATED, d.getInfo().getStatus());
		d.run();
		assertEquals(Integer.valueOf(0), d.getResult().getExitCode());
		//check proxy has been written out
		String workdir=d.getResult().getAction().getExecutionContext().getWorkingDirectory();
		TSI tsi=xnjs.getTargetSystemInterface(client,null);
		tsi.setStorageRoot(workdir);
		String proxyLoc=d.getResult().getAction().getExecutionContext().getEnvironment().get("X509_USER_PROXY");
		assertNotNull(proxyLoc);
		XnjsFile f=tsi.getProperties(proxyLoc);
		assertNotNull(f);
		assertEquals(proxy.length(),f.getSize());
		assertEquals(Status.DONE, d.getInfo().getStatus());
	}
	
	@Test
	public void testGSIDownloadErrors()throws Exception{
		xnjs.setProperty("XNJS.localtsi.useShell", "true");
		String proxy="proxies are bad for you";
		String dummyParent=createDummyParent();
		Client client=createClient(proxy);
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY,"/bin/no-such-executable");
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY_PARAMS, "args");
		GSIFTPDownload d=new GSIFTPDownload(client,"target",new URI("http://test"),"test",xnjs);
		d.getInfo().setParentActionID(dummyParent);
		assertEquals(Status.CREATED, d.getInfo().getStatus());
		d.run();
		d.getResult().getAction().printLogTrace();
		ExecutionContext ctx=d.getResult().getAction().getExecutionContext();
		//we are using shell mode, so bash will exit with code 127 when executable was not found
		assertEquals(Integer.valueOf(127), ctx.getExitCode());
		assertEquals(Status.FAILED, d.getInfo().getStatus());
		assertNotNull(d.getInfo().getStatusMessage());
	}
	
	@Test
	public void testGSIUpload()throws Exception{
		xnjs.setProperty("XNJS.localtsi.useShell", "false");
		String proxy="proxies are bad for you";
		String dummyParent=createDummyParent();
		String wd=getWorkingDir(dummyParent);
		Client client=createClient(proxy);
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY, "/bin/echo");
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY_PARAMS, "args");
		GSIFTPUpload d=new GSIFTPUpload(client,wd,"test",new URI("http://test"),xnjs);
		d.getInfo().setParentActionID(dummyParent);
		String cmd=d.makeCommandline();
		assertEquals("/bin/echo args 'file:"+getWorkingDir(dummyParent)+"test' 'http://test'",cmd);
		assertEquals(Status.CREATED, d.getInfo().getStatus());
		d.run();
		ExecutionContext ctx=d.getResult().getAction().getExecutionContext();
		assertEquals(Integer.valueOf(0), ctx.getExitCode());
		//check proxy has been written out
		TSI tsi=xnjs.getTargetSystemInterface(client,null);
		tsi.setStorageRoot(wd);
		String proxyLoc=ctx.getEnvironment().get("X509_USER_PROXY");
		XnjsFile f=tsi.getProperties(proxyLoc);
		assertNotNull(f);
		assertEquals(proxy.length(),f.getSize());
		assertEquals(Status.DONE, d.getInfo().getStatus());
	}
	
	@Test
	public void testGSIUploadErrors()throws Exception{
		xnjs.setProperty("XNJS.localtsi.useShell", "true");
		String proxy="proxies are bad for you";
		String dummyParent=createDummyParent();
		Client client=createClient(proxy);
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY,"/bin/no-such-executable");
		xnjs.getIOProperties().setProperty(IOProperties.GLOBUS_URL_COPY_PARAMS, "args");
		GSIFTPUpload d=new GSIFTPUpload(client,"target","test",new URI("http://test"),xnjs);
		d.getInfo().setParentActionID(dummyParent);
		assertEquals(Status.CREATED, d.getInfo().getStatus());
		d.run();
		ExecutionContext ctx=d.getResult().getAction().getExecutionContext();
		//we are using shell mode, so bash will exit with code 127 when executable was not found
		assertEquals(Integer.valueOf(127), ctx.getExitCode()); 
		assertEquals(Status.FAILED, d.getInfo().getStatus());
		assertNotNull(d.getInfo().getStatusMessage());
	}

	private String createDummyParent()throws Exception{
		JSONObject j = new JSONObject();
		j.put("ApplicationName","Date");
		Action job=xnjs.makeAction(j);
		String id = (String)mgr.add(job, null);
		waitUntilReady(id);
		return id;
	}

	private String getWorkingDir(String actionID)throws Exception{
		return internalMgr.getAction(actionID).getExecutionContext().getWorkingDirectory();
	}
	
	private Client createClient(String proxy){
		Client c=new Client();
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		SecurityTokens st=new SecurityTokens();
		if(proxy!=null)st.getContext().put("Proxy",proxy);
		st.setUserName("CN=test");
		c.setAuthenticatedClient(st);
		return c;
	}
}
