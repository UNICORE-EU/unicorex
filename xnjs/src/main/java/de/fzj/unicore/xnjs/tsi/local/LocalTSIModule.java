package de.fzj.unicore.xnjs.tsi.local;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.LocalECManager;
import de.fzj.unicore.xnjs.idb.GrounderImpl;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.IDBImpl;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.incarnation.ITweaker;
import de.fzj.unicore.xnjs.incarnation.IncarnationTweaker;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.impl.FileTransferEngine;
import de.fzj.unicore.xnjs.tsi.BasicExecution;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.IExecutionSystemInformation;
import de.fzj.unicore.xnjs.tsi.TSI;

public class LocalTSIModule extends AbstractModule {
	
	protected final Properties properties;
	
	public LocalTSIModule(Properties properties) {
		this.properties = properties;
	}

	@Provides
	public LocalTSIProperties getLocalTSIProperties(){
		return new LocalTSIProperties(properties);
	}

	@Override
	protected void configure(){

		bind(IExecutionContextManager.class).to(LocalECManager.class);
		bind(IExecution.class).to(BasicExecution.class);
		bind(IExecutionSystemInformation.class).to(BasicExecution.class);
		bind(TSI.class).to(LocalTS.class);
		
		bind(Incarnation.class).to(GrounderImpl.class);
		bind(ITweaker.class).to(IncarnationTweaker.class);
		bind(IDB.class).to(IDBImpl.class);
		
		bind(IFileTransferEngine.class).to(FileTransferEngine.class);
	}
}
