package de.fzj.unicore.uas.client;

import java.util.Map;

import de.fzj.unicore.uas.FiletransferParameterProvider;

public class MockFTParamsProvider implements FiletransferParameterProvider{

	@Override
	public void provideParameters(Map<String, String> params, String protocol) {
		if("TEST".equals(protocol)){
			params.put("TEST.foo","bar");
		}
	}


}
