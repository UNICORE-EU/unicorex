package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceTargetType;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.TSSClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * submit a job containing stage in/out sections
 * @author schuller
 */
public class TestStageInStageOut extends AbstractJobRun {

	String testString="this is a staging test";
	
	StorageClient helperUSpace;
	
	String uspaceUrl;
	
	String[]testedProtocols=new String[]{"BFT"};

	@FunctionalTest(id="StageInOutTest", 
					description="Tests job having stagein/stageout sections.")
	@Override
	protected JobClient submitJob(TSSClient tss)throws Exception{
		createHelperUspace();
		return super.submitJob(tss);
	}
	
	@Override
	protected void beforeStart(JobClient jms) throws Exception {
		jms.getUspaceClient().upload("test.txt").write(testString.getBytes());
	}
	
	@Override
	protected void onFinish(JobClient jms) throws Exception {
		// check stage ins
		GridFileType[]gft=jms.getUspaceClient().listDirectory("/");
		System.out.println("Staged in:");
		int simpleFiles=0;
		int directories=0;
		for(GridFileType g: gft){
			if(g.getPath().endsWith("test.txt"))continue;
			System.out.println(g.getPath()+" size: "+g.getSize());
			if(!g.getIsDirectory()){
				assertEquals(testString.length(),g.getSize());
				simpleFiles++;
			}
			else{
				directories++;
			}
			
		}
		assertEquals(testedProtocols.length,simpleFiles);
		assertEquals(testedProtocols.length,directories);
		
		//check stage outs
		gft=helperUSpace.listDirectory("/");
		System.out.println("Staged out:");
		simpleFiles=0;
		directories=0;
		int appended=0;
		for(GridFileType g: gft){
			if(g.getPath().endsWith("test.txt"))continue;
			System.out.println(g.getPath()+(g.getIsDirectory() ? " <dir>" : (" size: "+g.getSize())));
			if(g.getPath().contains("out_append_")){
				assertTrue(g.getSize()==2*testString.length());
				appended++;
			}
			else{
				if(!g.getIsDirectory()){
					assertEquals(testString.length(),g.getSize());
					simpleFiles++;
				}
				else{
					directories++;
				}
			}
		}
		assertEquals(testedProtocols.length,simpleFiles);
		assertEquals(testedProtocols.length,directories);
		assertEquals(testedProtocols.length,appended);
	}
	
	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription();
		//add stage-ins
		for(String protocol: testedProtocols){
			StringBuilder sb=new StringBuilder();
			DataStagingType s=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
			s.setFileName("/in_"+protocol);
			SourceTargetType stt=SourceTargetType.Factory.newInstance();
			sb.append(protocol);
			sb.append(":");
			sb.append(uspaceUrl);
			sb.append("#test.txt");
			stt.setURI(sb.toString());
			s.setSource(stt);
			s.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
			
			// stage a file into a directory (to test adding the parent dir)
			StringBuilder sb2=new StringBuilder();
			DataStagingType s2=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
			s2.setFileName("/in_dir_"+protocol+"/test.txt");
			SourceTargetType stt2=SourceTargetType.Factory.newInstance();
			sb2.append(protocol);
			sb2.append(":");
			sb2.append(uspaceUrl);
			sb2.append("#test.txt");
			stt2.setURI(sb2.toString());
			s2.setSource(stt2);
			s2.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		}
		
		//add stage-outs
		for(String protocol: testedProtocols){
			StringBuilder sb=new StringBuilder();
			DataStagingType s=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
			s.setFileName("/test.txt");
			SourceTargetType stt=SourceTargetType.Factory.newInstance();
			sb.append(protocol);
			sb.append(":");
			sb.append(uspaceUrl);
			sb.append("#out_");
			sb.append(protocol);
			stt.setURI(sb.toString());
			s.setTarget(stt);
			s.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
			
			//stage out into a sub-directory
			StringBuilder sb2=new StringBuilder();
			DataStagingType s2=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
			s2.setFileName("/test.txt");
			SourceTargetType stt2=SourceTargetType.Factory.newInstance();
			sb2.append(protocol);
			sb2.append(":");
			sb2.append(uspaceUrl);
			sb2.append("#out_");
			sb2.append(protocol);
			sb2.append("_dir/out_");
			sb2.append(protocol);
			stt2.setURI(sb2.toString());
			s2.setTarget(stt2);
			s2.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
			
		}
		
		//add stage-outs where files are to be appended
		for(String protocol: testedProtocols){
			StringBuilder sb=new StringBuilder();
			DataStagingType s=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
			s.setFileName("/test.txt");
			SourceTargetType stt=SourceTargetType.Factory.newInstance();
			sb.append(protocol);
			sb.append(":");
			sb.append(uspaceUrl);
			sb.append("#out_append_");
			sb.append(protocol);
			stt.setURI(sb.toString());
			s.setTarget(stt);
			s.setCreationFlag(CreationFlagEnumeration.APPEND);
		}
		
		return jdd;
	}
	
	protected void createHelperUspace()throws Exception{
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription();
		SubmitDocument in=SubmitDocument.Factory.newInstance();
		in.addNewSubmit().setJobDefinition(jdd.getJobDefinition());
		JobClient jc=tss.submit(in);
		helperUSpace=jc.getUspaceClient();
		uspaceUrl=helperUSpace.getEPR().getAddress().getStringValue();
		helperUSpace.upload("test.txt").write(testString.getBytes());
		//for testing the overwrite behaviour on stage-out
		for(String protocol: testedProtocols){
			helperUSpace.upload("out_"+protocol).append(testString.getBytes());
			helperUSpace.copy("out_"+protocol, "out_append_"+protocol);
		}
	}
}
