package eu.unicore.xnjs.tsi.remote;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.SlidingWindowReservoir;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import eu.unicore.util.configuration.UpdateableConfiguration;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJSConstants;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.ems.LocalECManager;
import eu.unicore.xnjs.idb.GrounderImpl;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.idb.IDBImpl;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.impl.FileTransferEngine;
import eu.unicore.xnjs.tsi.IExecution;
import eu.unicore.xnjs.tsi.IExecutionSystemInformation;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.TSI;

public class RemoteTSIModule extends AbstractModule
implements ConfigurationSource.MetricProvider, UpdateableConfiguration {
	
	protected Properties properties;

	private TSIProperties tsiProps;

	protected final Histogram mtq = new Histogram(new SlidingWindowReservoir(512));

	public RemoteTSIModule(Properties properties) {
		this.properties = properties;
	}

	@Override
	public void setProperties(Properties newProperties) {
		this.properties = newProperties;
		tsiProps.setProperties(newProperties);
	}

	@Override
	protected void configure(){
		bind(IExecutionContextManager.class).to(LocalECManager.class);
		bind(TSIConnectionFactory.class).to(DefaultTSIConnectionFactory.class);
		bind(TSI.class).to(RemoteTSI.class);
		bind(Incarnation.class).to(GrounderImpl.class);
		bind(IFileTransferEngine.class).to(FileTransferEngine.class);
		bindIDB();
		bind(IBSSState.class).to(BSSState.class);
		bindReservation();
		bindExecution();
	}
	
	@Provides
	public TSIProperties getTSIProperties(){
		if(tsiProps==null) {
			tsiProps = new TSIProperties(properties);
		}
		return tsiProps;
	}
	
	@Provides
	public Histogram getMeanTimeQueued(){
		return mtq;
	}

	@Override
	public Map<String, Metric> getMetrics(){
		Map<String, Metric> m = new HashMap<>();
		m.put(XNJSConstants.MEAN_TIME_QUEUED, mtq);
		return m;
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
