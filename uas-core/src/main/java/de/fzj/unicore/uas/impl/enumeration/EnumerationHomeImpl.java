package de.fzj.unicore.uas.impl.enumeration;

import eu.unicore.services.Resource;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

public class EnumerationHomeImpl extends WSResourceHomeImpl {

	@Override
	protected Resource doCreateInstance() {
		return new EnumerationImpl();
	}

}
