package de.fzj.unicore.uas.sshtsi;

import java.util.Properties;

import com.google.inject.Provides;

import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.LocalECManager;
import de.fzj.unicore.xnjs.idb.GrounderImpl;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.incarnation.ITweaker;
import de.fzj.unicore.xnjs.incarnation.IncarnationTweaker;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.impl.FileTransferEngine;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.remote.RemoteTSI;
import de.fzj.unicore.xnjs.tsi.remote.RemoteTSIModule;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnectionFactory;

public class SSHTSIModule extends RemoteTSIModule {

	public SSHTSIModule(Properties properties) {
		super(properties);
	}
	
	@Override
	protected void configure(){

		bind(IExecutionContextManager.class).to(LocalECManager.class);
		bind(TSIConnectionFactory.class).to(SSHTSIConnectionFactory.class);
		bind(TSI.class).to(RemoteTSI.class);
		
		bind(Incarnation.class).to(GrounderImpl.class);
		bind(ITweaker.class).to(IncarnationTweaker.class);		

		bind(IFileTransferEngine.class).to(FileTransferEngine.class);

		bindIDB();
		bindReservation();
		bindExecution();
	}
	
	
	@Provides
	public SSHTSIProperties getSSHTSIProperties(){
		return new SSHTSIProperties(properties);
	}
	
}
