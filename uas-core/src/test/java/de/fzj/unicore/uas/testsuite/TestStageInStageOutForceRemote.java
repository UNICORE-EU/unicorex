package de.fzj.unicore.uas.testsuite;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.TSSClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * submit a job containing stage in/out sections
 * @author schuller
 */
public class TestStageInStageOutForceRemote extends TestStageInStageOut{

	@FunctionalTest(id="StageInOutTestRemote", 
			description="Tests job having stagein/stageout sections forcing remote data transfer.")
	@Override
	protected JobClient submitJob(TSSClient tss)throws Exception{
		UASProperties cfg = kernel.getAttribute(UASProperties.class);
		cfg.setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE,"true");
		return super.submitJob(tss);
	}
	
}
