package de.fzj.unicore.uas.impl.task;

import eu.unicore.services.Resource;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

public class TaskHomeImpl extends WSResourceHomeImpl {

	@Override
	protected Resource doCreateInstance() {
		return new TaskImpl();
	}

}
