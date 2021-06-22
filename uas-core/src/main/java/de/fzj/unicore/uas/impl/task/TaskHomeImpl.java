package de.fzj.unicore.uas.impl.task;

import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class TaskHomeImpl extends DefaultHome {

	@Override
	protected Resource doCreateInstance() {
		return new TaskImpl();
	}

}
