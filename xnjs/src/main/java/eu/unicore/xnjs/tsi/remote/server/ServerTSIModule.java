package eu.unicore.xnjs.tsi.remote.server;

import java.util.Properties;

import eu.unicore.xnjs.tsi.remote.RemoteTSIModule;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;

public class ServerTSIModule extends RemoteTSIModule {

	public ServerTSIModule(Properties properties) {
		super(properties);
	}

	@Override
	protected Class<? extends TSIConnectionFactory> getConnectionFactory(){
		return DefaultTSIConnectionFactory.class;
	}
	
}
