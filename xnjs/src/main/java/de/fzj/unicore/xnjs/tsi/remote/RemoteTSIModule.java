package de.fzj.unicore.xnjs.tsi.remote;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.LocalECManager;
import de.fzj.unicore.xnjs.idb.GrounderImpl;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.IDBImpl;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.impl.FileTransferEngine;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.IExecutionSystemInformation;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.TSI;

public class RemoteTSIModule extends AbstractModule {
	
	protected final Properties properties;
	private final XNJS xnjs;
	private IBSSState bssState;

	public RemoteTSIModule(Properties properties, XNJS xnjs) {
		this.properties = properties;
		this.xnjs = xnjs;
	}

	@Override
	protected void configure(){
		bind(IExecutionContextManager.class).to(LocalECManager.class);
		bind(TSIConnectionFactory.class).to(DefaultTSIConnectionFactory.class);
		bind(TSI.class).to(RemoteTSI.class);
		bind(Incarnation.class).to(GrounderImpl.class);
		bind(IFileTransferEngine.class).to(FileTransferEngine.class);
		bindIDB();
		bindReservation();
		bindExecution();
	}
	
	@Provides
	public TSIProperties getTSIProperties(){
		return new TSIProperties(properties);
	}
	
	@Provides
	public synchronized IBSSState getBSSState(){
		if(bssState==null) {
			
		}
		return bssState;
	}
	
	protected void bindExecution(){
		bind(IExecution.class).to(Execution.class);
		bind(IExecutionSystemInformation.class).to(Execution.class);
	}
	
	protected void bindReservation(){
		bind(IReservation.class).to(Reservation.class);
	}
	
	protected void bindIDB() {
		bind(IDB.class).to(IDBImpl.class);
	}
}
