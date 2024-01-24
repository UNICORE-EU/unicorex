package de.fzj.unicore.uas.xnjs;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import eu.unicore.security.Client;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.ems.LocalECManager;
import eu.unicore.xnjs.idb.GrounderImpl;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.idb.IDBImpl;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.impl.FileTransferEngine;
import eu.unicore.xnjs.tsi.BasicExecution;
import eu.unicore.xnjs.tsi.IExecution;
import eu.unicore.xnjs.tsi.IExecutionSystemInformation;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.local.LocalTS;
import eu.unicore.xnjs.tsi.local.LocalTSIModule;

public class MyLocalTSIModule extends LocalTSIModule {

	public MyLocalTSIModule(Properties properties) {
		super(properties);
	}

	@Override
	protected void configure(){
		bind(IExecutionContextManager.class).to(LocalECManager.class);
		bind(IExecution.class).to(BasicExecution.class);
		bind(IExecutionSystemInformation.class).to(MyExecution.class);
		bind(TSI.class).to(LocalTS.class);
		
		bind(Incarnation.class).to(GrounderImpl.class);
		bind(IDB.class).to(IDBImpl.class);
		
		bind(IFileTransferEngine.class).to(FileTransferEngine.class);
		bind(IReservation.class).to(MockReservation.class);
	}
	
	public static class MyExecution extends BasicExecution{
		
		@Override
		public List<BudgetInfo>getComputeTimeBudget(Client c) throws ExecutionException {
			List<BudgetInfo> budget = new ArrayList<>();
			budget.add(new BudgetInfo("hpc 1000 10 core-h"));
			budget.add(new BudgetInfo("test 10000 20 core-h"));
			return budget;
		}
		
	}
	
}
