package eu.unicore.xnjs.tsi.local;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import eu.unicore.util.configuration.UpdateableConfiguration;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.ems.LocalECManager;
import eu.unicore.xnjs.fts.IUFTPRunner;
import eu.unicore.xnjs.idb.GrounderImpl;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.idb.IDBImpl;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.impl.FileTransferEngine;
import eu.unicore.xnjs.tsi.BasicExecution;
import eu.unicore.xnjs.tsi.IExecution;
import eu.unicore.xnjs.tsi.IExecutionSystemInformation;
import eu.unicore.xnjs.tsi.TSI;

public class LocalTSIModule extends AbstractModule 
implements UpdateableConfiguration {

	protected Properties properties;

	private LocalTSIProperties tsiProps;

	public LocalTSIModule(Properties properties) {
		this.properties = properties;
	}

	@Provides
	public LocalTSIProperties getLocalTSIProperties(){
		if(tsiProps==null) {
			tsiProps = new LocalTSIProperties(properties);
		}
		return tsiProps;
	}

	@Override
	public void setProperties(Properties newProperties) {
		this.properties = newProperties;
		tsiProps.setProperties(newProperties);
	}

	@Override
	protected void configure(){
		bind(IExecutionContextManager.class).to(LocalECManager.class);
		bind(IExecution.class).to(BasicExecution.class);
		bind(IExecutionSystemInformation.class).to(BasicExecution.class);
		bind(TSI.class).to(LocalTS.class);

		bind(Incarnation.class).to(GrounderImpl.class);
		bind(IDB.class).to(IDBImpl.class);

		bind(IFileTransferEngine.class).to(FileTransferEngine.class);
		bind(IUFTPRunner.class).to(UFTPRunnerImpl.class);
	}
}
