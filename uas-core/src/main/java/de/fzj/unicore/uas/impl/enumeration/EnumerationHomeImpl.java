package de.fzj.unicore.uas.impl.enumeration;

import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class EnumerationHomeImpl extends DefaultHome {

	@Override
	protected Resource doCreateInstance() {
		return new EnumerationImpl();
	}

}
