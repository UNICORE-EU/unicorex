package eu.unicore.xnjs.tsi.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.logging.log4j.Logger;

import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.xnjs.util.IOUtils;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Helper class to run job, used by the embedded TSI {@link LocalTS}<br/>
 * 
 * @author schuller
 * @see LocalTS
 */
public class LocalExecution implements Runnable {

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,LocalExecution.class);

	private final String actionID;
	private final String cmd;
	private final String workDir;
	private final ExecutionContext ec;
	private final InternalManager manager;
	private final LocalTSIProperties tsiProperties;
	
	private static int rejected=0;

	private static final AtomicInteger completedTasks=new AtomicInteger(0);
	private static final AtomicInteger runningTasks=new AtomicInteger(0);
	private static final AtomicInteger totalTasks=new AtomicInteger(0);

	/**
	 * local TS uses a pool of execution threads of fixed size
	 */
	private static ThreadPoolExecutor es;

	/**
	 * helper pool for dealing with process in/out/err streams
	 */
	private static ThreadPoolExecutor es2;

	public static int getExecutorPoolMinSize(){
		return es.getCorePoolSize();
	}

	public static int getExecutorPoolMaxSize(){
		return es.getMaximumPoolSize();
	}

	public static long getCompletedTasks(){
		return completedTasks.longValue();
	}

	public static int getActiveTasks(){
		return es.getActiveCount();
	}

	private static final Set<String> runningJobs=Collections.synchronizedSet(new HashSet<String>());
	private static final Map<String,Integer> exitCodes=new ConcurrentHashMap<String,Integer>();
	private static final Map<String,Process> processes=new ConcurrentHashMap<String,Process>();

	public static boolean isRunning(String actionID){
		return runningJobs.contains(actionID); 
	}

	public static Integer getExitCode(String actionID){
		if(actionID!=null){
			Integer exit=exitCodes.get(actionID);
			if(exit!=null){
				exitCodes.remove(actionID);
				return exit;
			}
		}
		return null; 
	}

	private synchronized void initPool(){
		if(es!=null)return;
		int size = tsiProperties.getNumberOfThreads();
		es=new ThreadPoolExecutor(size, size, 
				60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		es2=new ThreadPoolExecutor(2*size, 2*size, 
				60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

	}

	/**
	 * @param actionID - can be null
	 * @param properties
	 * @param manager
	 * @param cmd
	 * @param ec
	 */
	public LocalExecution(String actionID, LocalTSIProperties properties, InternalManager manager, String cmd, ExecutionContext ec){
		this.manager = manager;
		this.tsiProperties = properties;
		this.actionID = actionID;
		this.cmd=cmd;
		this.workDir = ec.getWorkingDirectory();
		this.ec=ec;
		initPool();
	}

	/**
	 * execute asynchronously
	 */
	public void execute() {
		execute(true);
	}

	/**
	 * execute this task
	 * @param async - if true, the task is submitted to the executor service, if false, it is run in the calling thread
	 */
	public void execute(boolean async) {
		totalTasks.incrementAndGet();
		if(async){
			es.execute(this);
			if(actionID!=null){
				runningJobs.add(actionID);
			}
		}
		else {
			if(actionID!=null){
				runningJobs.add(actionID);
			}
			run();
		}
	}

	public void run() {

		OutputStream stdout=null;
		OutputStream stderr=null;
		InputStream stdin=null;
		OutputStream stdinDummy=null;
		
		boolean redirectInput=false;

		try{
			long start=System.currentTimeMillis();
			runningTasks.incrementAndGet();
			String what=replaceEnvVars(cmd, ec.getEnvironment());
			logger.info("["+actionID+"] Executing '"+what+"' in "+workDir);
			ProcessBuilder pb=new ProcessBuilder(); 
			pb.directory(new File(workDir));
			copyEnv(ec.getEnvironment(),pb.environment());
			if(ec.getStdin()!=null){
				stdin=new FileInputStream(ec.getWorkingDirectory()+File.separator+ec.getStdin());
				redirectInput=true;
				logger.debug("Redirected input from '"+ec.getStdin()+"'");

				//open for writing as well (to support FIFOs properly)
				stdinDummy=new FileOutputStream(ec.getWorkingDirectory()+File.separator+ec.getStdin(),true);
			}
			pb.command(createCommandArray(what));

			Process p=pb.start();

			if(actionID!=null) {
				processes.put(actionID, p);
				try{
				//send continue event so the status gets updated
					ContinueProcessingEvent cpe=new ContinueProcessingEvent(actionID);
					manager.handleEvent(cpe);
				}catch(Exception ex){
					LogUtil.logException("Error sending continue event", ex, logger);
				}
			}

			//in
			if(redirectInput){
				DataMover in=new DataMover(stdin, p.getOutputStream());
				es2.execute(in);
			}

			//write stdout/stderr files
			DataMover out=null;
			DataMover err=null;

			//err
			stderr=ec.isDiscardOutput() ? NullOutputStream.INSTANCE
					 : new FileOutputStream(ec.getOutputDirectory()+File.separator+ec.getStderr());
			err=new DataMover(p.getErrorStream(),stderr);

			while(true){
				try{
					es2.execute(err);
					break;
				}catch(RejectedExecutionException re){
					try{
						Thread.sleep(1000);
					}catch(InterruptedException ignored){}
				}
			}
			//out
			stdout=ec.isDiscardOutput() ? NullOutputStream.INSTANCE
			        : new FileOutputStream(ec.getOutputDirectory()+File.separator+ec.getStdout());
			out=new DataMover(p.getInputStream(),stdout);
			out.run();

			int exit=p.waitFor();

			//wait for error/output writes to terminate
			while(! (out.isDone() && err.isDone()) ){
				Thread.sleep(10);
			}

			long diff=System.currentTimeMillis()-start;
			logger.info("["+actionID+"] Done with '"+what+"' exit code "+exit+", took "+diff+" ms.");
			completedTasks.incrementAndGet();

			//save exit code
			ec.setExitCode(exit);
			if(actionID!=null)exitCodes.put(actionID, Integer.valueOf(exit));
			try{
				p.destroy();
			}catch(Exception e){
				logger.warn("Error while destroying process.",e);
			}
			pb=null;
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		finally{
			
			IOUtils.closeQuietly(stdout);
			IOUtils.closeQuietly(stderr);
			IOUtils.closeQuietly(stdin);
			IOUtils.closeQuietly(stdinDummy);
			
			if(actionID!=null){
				runningJobs.remove(actionID);
				runningTasks.decrementAndGet();
				totalTasks.decrementAndGet();
				
				try{
					manager.handleEvent(new ContinueProcessingEvent(actionID));
				}catch(Exception ex){
					logger.error("Could not send status change notification",ex);
				}
			}
		}
	}

	//TODO: fix program names containing space characters 
	protected String[] createCommandArray(String commandLine){
		return commandLine.split(" +");
	}

	// ---  helpers

	private String replaceEnvVars(String original, Map<String,String>env){
		String sb=original;
		String[] keys=env.keySet().toArray(new String[0]);
		//need to make sure we replace in the proper order 
		//e.g. with vars FOO and FOOBAR we first need to replace FOOBAR
		Arrays.sort(keys);
		for(int i=keys.length-1;i>=0;i--){
			String e=keys[i];
			sb=sb.replaceAll("\\$"+e, ec.getEnvironment().get(e) );
			sb=sb.replaceAll("\\$\\{"+e+"\\}", ec.getEnvironment().get(e) );
		}
		return sb;
	}

	private void copyEnv(Map<String,String> from, Map<String,String> to){
		for(Map.Entry<String,String> e: from.entrySet()){
			to.put(e.getKey(), e.getValue());
		}
	}

	public static Set<String> getRunningJobs(){
		return runningJobs;
	}

	public static int getNumberOfRejectedTasks(){
		return rejected;
	}

	public static int getNumberOfRunningJobs() {
		return runningTasks.get();
	}


	public static int getTotalNumberOfJobs() {
		return totalTasks.get();
	}


	public static void abort(String uuid)throws ExecutionException{
		try{
			Process p=processes.get(uuid);
			if(p!=null){
				p.destroy();
			}
		}
		catch(Exception e){
			logger.error("Could not destroy process.",e);
			throw new ExecutionException(e);
		}
	}
	
	public static void reset() {
		try {
			 completedTasks.getAndSet(0);
			 runningTasks.getAndSet(0);
			 totalTasks.getAndSet(0);
			 rejected = 0;
		}catch(Exception e) {}
	}

	public static class DataMover implements Runnable{

		private final InputStream source;

		private final OutputStream target;

		private final byte[] buf;

		private Exception exception;

		public DataMover(InputStream source, OutputStream target){
			buf=new byte[1024];
			this.source=source;
			this.target=target;
		}

		private boolean done=false;

		public boolean isDone() throws Exception{
			if(exception!=null)throw exception;
			return done;
		}

		public void run(){
			int c;
			while(!done){
				try{
					c=source.read(buf);
					if(c==-1){
						done=true;
					}else if(c>0){
						target.write(buf,0,c);
					}
					else{
						Thread.yield();
					}
					target.flush();
				}catch(Exception e){
					exception=e;
					done=true;
				}
			}
		}
	}

}


