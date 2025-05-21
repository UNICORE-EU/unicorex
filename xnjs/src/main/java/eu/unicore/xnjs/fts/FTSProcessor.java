package eu.unicore.xnjs.fts;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.services.restclient.utils.UnitParser;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.processors.DefaultProcessor;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.DataStagingInfo;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.IOProperties;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.json.JSONParser;
import eu.unicore.xnjs.util.JSONUtils;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Processor for reliable, multi-file server-to-server file transfers<br/>
 * 
 * Creates and manages a set of IFileTransfer instances, each responsible for a single file,
 * restarting them if needed.
 * 
 * @author schuller
 */
public class FTSProcessor extends DefaultProcessor {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS, FTSProcessor.class);

	public FTSProcessor(XNJS xnjs){
		super(xnjs);
	}


	private JSONObject ftSpec;

	protected JSONObject getTransferSpecification(){
		if(ftSpec==null) {
			try{
				ftSpec = new JSONObject((String)action.getAjd());
			}catch(Exception ex) {}
		}
		return ftSpec;
	}

	protected boolean isExport(JSONObject ftSpec) {
		return ftSpec.optString("target", null)!=null;
	}

	/**
	 * Initiates the filetransfer
	 */
	protected void handleCreated() throws Exception {
			IFTSController ft = getController();
			List<FTSTransferInfo> fileList = new ArrayList<>();
			long totalSize = ft.collectFilesForTransfer(fileList);
			FTSInfo info = new FTSInfo(action.getUUID());
			info.setTotalSize(totalSize);
			info.setRunningTransfers(0);
			info.setTransfers(fileList);
			action.addLogTrace("Have <"+fileList.size()+"> files, total size: "
					+ UnitParser.getCapacitiesParser(2).getHumanReadable(totalSize));
			action.setStatus(ActionStatus.RUNNING);
			storeFTSInfo(info);
	}	

	private IFTSController ftc;

	protected IFTSController getController() throws Exception {
		if(ftc!=null)return ftc;
		JSONObject ftSpec = getTransferSpecification();
		if(isExport(ftSpec)){
			DataStageOutInfo info = new DataStageOutInfo();
			info.setFileName(ftSpec.getString("file"));
			info.setTarget(new URI(urlEncode(ftSpec.getString("target"))));
			configureCommon(info, ftSpec);
			ftc = xnjs.get(IFileTransferEngine.class).
					createFTSExport(
							action.getClient(),
							ftSpec.getString("workdir"),
							info);
		}
		else{
			DataStageInInfo info = new DataStageInInfo();
			info.setFileName(ftSpec.getString("file"));
			info.setSources(new URI[]{new URI(urlEncode(ftSpec.getString("source")))});
			info.setInlineData(ftSpec.optString("data", null));
			configureCommon(info, ftSpec);
			ftc = xnjs.get(IFileTransferEngine.class).
					createFTSImport(
							action.getClient(),
							ftSpec.getString("workdir"),
							info);
		}
		return ftc;
	}

	private void configureCommon(DataStagingInfo info, JSONObject spec) throws Exception {
		JSONObject creds = ftSpec.optJSONObject("credentials");
		if(creds!=null)info.setCredentials(JSONParser.extractCredentials(creds));
		JSONObject extra = ftSpec.optJSONObject("extraParameters");
		if(extra!=null)info.setExtraParameters(JSONUtils.asStringMap(extra));
		info.setOverwritePolicy(OverwritePolicy.OVERWRITE);
	}

	protected FTSInfo getFTSInfo() throws Exception {
		return xnjs.get(IFileTransferEngine.class).getFTSStorage().read(action.getUUID());
	}

	protected void storeFTSInfo(FTSInfo info) throws Exception  {
		xnjs.get(IFileTransferEngine.class).getFTSStorage().write(info);
	}

	@Override
	protected void handleAborting()throws ExecutionException{
		try {
			List<FTSTransferInfo> ftList = getFTSInfo().getTransfers();
			if(ftList==null)throw new IllegalStateException("Filetransfer list not found in context");
			Iterator<FTSTransferInfo>iter = ftList.iterator();
			while(iter.hasNext()){
				String ftId = iter.next().getTransferUID();
				if(ftId!=null) {
					xnjs.get(IFileTransferEngine.class).abort(ftId);	
				}
			}
			super.handleAborting();
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	@Override
	protected void handleRunning() throws Exception {
		FTSInfo info = getFTSInfo();
		try {
			List<FTSTransferInfo> ftList = info.getTransfers();
			if(ftList==null)throw new IllegalStateException("Filetransfer list not found in context");
			Iterator<FTSTransferInfo>iter = ftList.iterator();
			int running = info.getRunningTransfers();
			logger.trace("RUNNING <{}> have <{}>", action.getUUID(), ftList.size());
			while(iter.hasNext()){
				FTSTransferInfo ftInfo = iter.next();
				logger.trace(ftInfo);
				Status status = ftInfo.getStatus();

				switch(status) {
				case CREATED:
					if(launchFiletransfer(ftInfo, running)==Status.RUNNING) {
						running = running + 1;
					}
					continue;
				case RUNNING:
					if(checkRunning(ftInfo)!=Status.RUNNING) {
						running = running - 1;
					}
					continue;
				case FAILED:
				case DONE:
				case ABORTED:
					continue;
				}
			}
			info.setRunningTransfers(running);

			if(running==0){
				action.addLogTrace("All transfers finished.");
				boolean success = true;
				boolean aborted = false;
				StringBuilder errors = new StringBuilder();
				for(FTSTransferInfo tr: ftList) {
					if(tr.getStatus()==Status.FAILED) {
						success = false;
						if(errors.length()>0)errors.append("; ");
						errors.append(tr.getStatusMessage());
					}
					if(tr.getStatus()==Status.ABORTED) {
						aborted = true;
						if(errors.length()>0)errors.append("; ");
						errors.append("User aborted");
					}
				}
				
				action.setStatus(ActionStatus.DONE);
				action.getResult().setErrorMessage(errors.toString());
				if(aborted) {
					action.getResult().setStatusCode(ActionResult.USER_ABORTED);
				}
				else if(!success) {
					action.getResult().setStatusCode(ActionResult.NOT_SUCCESSFUL);
				}
				else {
					action.getResult().setStatusCode(ActionResult.SUCCESSFUL);
				}
				logger.debug("File transfers <{}> done.", action.getUUID());
			}
			else {
				sleep(5, TimeUnit.MILLISECONDS);
			}
		}
		finally {
			if(info!=null)storeFTSInfo(info);
		}
	}

	protected Status launchFiletransfer(FTSTransferInfo info, Integer running) throws Exception {
		if(running > getNumberOfFiletransferThreads())return Status.CREATED;
		IFileTransfer ft = getController().createTransfer(info.getSource(), info.getTarget());
		if(ft==null) {
			throw new ExecutionException(0, "Cannot create file transfer instance for transfer <"+info+">");
		}
		xnjs.get(IFileTransferEngine.class).registerFileTransfer(ft);
		getExecutor().execute(ft);
		info.setStatus(Status.RUNNING);
		info.setTransferUID(ft.getInfo().getUniqueId());
		return Status.RUNNING;
	}

	protected Status checkRunning(FTSTransferInfo info) {
		String ftId = info.getTransferUID();
		TransferInfo ft = getInfo(ftId);
		if(ft==null){
			//TODO
			throw new IllegalStateException("File transfer '"+ftId+"' not found!");
		}
		if(ft.getStatus()==Status.DONE){
			logger.debug("File transfer {} SUCCESSFUL.", ftId);
			xnjs.get(IFileTransferEngine.class).cleanup(ftId);
			info.setStatus(Status.DONE);
			if(ft.getTransferredBytes()!=info.getSource().getSize()) {
				info.getSource().setSize(ft.getTransferredBytes());
			}
		}
		else if(ft.getStatus()==Status.FAILED){
			logger.debug("File transfer {} FAILED.", ft.getUniqueId());
			info.setStatus(Status.FAILED);
			info.setStatusMessage(ft.getStatusMessage());
		}
		return info.getStatus();
	}

	protected TransferInfo getInfo(String id){
		return  xnjs.get(IFileTransferEngine.class).getInfo(id);
	}

	private static Executor exec=null;

	/**
	 * get an {@link Executor} for running filetransfers. 
	 * This default implementation uses a thread pool with a fixed number of threads, as
	 * given by {@link #getNumberOfFiletransferThreads()}
	 * @return {@link Executor}
	 */
	protected synchronized Executor getExecutor(){
		if(exec==null){
			ThreadFactory tf=new ThreadFactory(){
				private final AtomicInteger number=new AtomicInteger(0);
				@Override
				public Thread newThread(Runnable r) {
					Thread t=new Thread(r);
					t.setName("XNJS-Filetransfers-"+number.incrementAndGet());
					return t;
				}
			};
			ThreadPoolExecutor exec1=new ThreadPoolExecutor(getNumberOfFiletransferThreads(), getNumberOfFiletransferThreads(), 
					10000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), tf);
			exec1.allowCoreThreadTimeOut(true);
			exec=exec1;
		}
		return exec;
	}

	/**
	 * get the maximum number of threads in the thread pool to be used for file transfers.
	 * This corresponds to the maximum number of concurrent file transfers
	 * @return maximum number of concurrent filetransfers, the default number is 4
	 */
	protected int getNumberOfFiletransferThreads(){
		return xnjs.getIOProperties().getIntValue(IOProperties.FT_THREADS);
	}

	public static String urlEncode(String orig){
		try{
			return orig.replaceAll(" ", "%20");
		}catch(Exception e){
			return orig;
		}
	}
}