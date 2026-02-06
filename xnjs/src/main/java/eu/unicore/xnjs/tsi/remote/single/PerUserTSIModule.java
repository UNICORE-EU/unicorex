package eu.unicore.xnjs.tsi.remote.single;

import java.util.Properties;

import com.google.inject.Provides;

import eu.unicore.xnjs.tsi.remote.RemoteTSIModule;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;

public class PerUserTSIModule extends RemoteTSIModule {

	protected PerUserTSIProperties perUserTSIProps;

	public PerUserTSIModule(Properties properties) {
		super(properties);
	}

	@Override
	protected Class<? extends TSIConnectionFactory> getConnectionFactory(){
		return PerUserTSIConnectionFactory.class;
	}

	@Provides
	public PerUserTSIProperties getPerUserTSIProperties(){
		if(perUserTSIProps==null) {
			perUserTSIProps = new PerUserTSIProperties(properties);
		}
		return perUserTSIProps;
	}


	@Override
	protected void configure(){
		super.configure();
		bind(IdentityStore.class).to(DefaultIdentityStore.class);
	}

}