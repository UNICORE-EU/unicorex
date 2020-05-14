package de.fzj.unicore.uas.impl.enumeration;

import de.fzj.unicore.wsrflite.Resource;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

public class EnumerationHomeImpl extends WSResourceHomeImpl {

	@Override
	protected Resource doCreateInstance() {
		return new EnumerationImpl();
	}

}
