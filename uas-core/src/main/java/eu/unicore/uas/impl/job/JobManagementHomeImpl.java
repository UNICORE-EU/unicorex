package eu.unicore.uas.impl.job;

import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class JobManagementHomeImpl extends DefaultHome {
	
	@Override
	protected Resource doCreateInstance() {
		return new JobManagementImpl();
	}

}
