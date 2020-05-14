package de.fzj.unicore.uas.xnjs;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.ems.ExecutionException;
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
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.local.LocalTS;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.security.Client;

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
		bind(ITweaker.class).to(IncarnationTweaker.class);
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
