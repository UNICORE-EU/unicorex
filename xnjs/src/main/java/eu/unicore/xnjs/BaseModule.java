package eu.unicore.xnjs;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.IPlainClientConfiguration;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.io.http.IConnectionFactory;
import eu.unicore.xnjs.io.http.SimpleConnectionFactory;

/**
 * common components setup
 * 
 * @author schuller
 */
public class BaseModule extends AbstractModule {

	protected final Properties properties;
	
	public BaseModule(Properties properties){
		this.properties = properties;
	}
	
	@Override
	protected void configure(){
		bind(InternalManager.class).to(BasicManager.class);
		bind(Manager.class).to(BasicManager.class);
	}
	
	@Provides
	public IConnectionFactory getConnectionFactory(){
		return new SimpleConnectionFactory();
	}
	
	@Provides
	public IClientConfiguration getSecurityConfiguration(){
		return new DefaultClientConfiguration();
	}
	
	@Provides
	public IPlainClientConfiguration getSecurityConfiguration2(){
		return getSecurityConfiguration();
	}

}
