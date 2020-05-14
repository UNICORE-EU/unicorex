package de.fzj.unicore.uas.impl.task;

import de.fzj.unicore.wsrflite.Resource;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

public class TaskHomeImpl extends WSResourceHomeImpl {

	@Override
	protected Resource doCreateInstance() {
		return new TaskImpl();
	}

}
