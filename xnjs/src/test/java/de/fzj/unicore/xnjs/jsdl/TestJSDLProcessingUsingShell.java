package de.fzj.unicore.xnjs.jsdl;

import de.fzj.unicore.xnjs.ConfigurationSource;

public class TestJSDLProcessingUsingShell extends TestJSDLProcessing{

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS.localtsi.useShell","true");
	}

}
