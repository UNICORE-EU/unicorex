package eu.unicore.xnjs.tsi.remote;

import java.io.IOException;

import jakarta.inject.Inject;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import eu.unicore.xnjs.fts.IUFTPRunner;
import eu.unicore.xnjs.util.AsyncCommandHelper;
import eu.unicore.xnjs.util.ResultHolder;
import eu.unicore.xnjs.util.UFTPUtils;

public class UFTPRunnerImpl implements IUFTPRunner {

	private Client client;

	private String parentActionID;

	private String id;

	private String preferredTSINode;

	private final XNJS xnjs;

	private String subactionID;

	@Inject
	public UFTPRunnerImpl(XNJS xnjs) {
		this.xnjs = xnjs;
	}

	@Override
	public void setClient(Client client) {
		this.client = client;
	}

	@Override
	public void setID(String id) {
		this.id = id;
	}

	@Override
	public void setClientHost(String host) {
		this.preferredTSINode = host;
	}

	@Override
	public void setParentActionID(String actionID) {
		this.parentActionID = actionID;
	}

	@Override
	public void get(String from, String to, String workdir, String host, int port, String secret) throws Exception {
		String cmd = UFTPUtils.jsonBuilder()
				.get().from(from).to(to).workdir(workdir)
				.secret(secret)
				.host(host).port(port)
				.build().toString();
		runAsync(cmd);
	}

	@Override
	public void put(String from, String to, String workdir, String host, int port, String secret) throws Exception {
		String cmd = UFTPUtils.jsonBuilder()
				.put().from(from).to(to).workdir(workdir)
				.secret(secret)
				.host(host).port(port)
				.build().toString();
		runAsync(cmd);
	}

	@Override
	public String getSubactionID() {
		return subactionID;
	}

	private AsyncCommandHelper ach;

	private void runAsync(String cmd) throws Exception {
		ach = new AsyncCommandHelper(xnjs, cmd, id, parentActionID, client);
		ach.setPreferredExecutionHost(preferredTSINode);
		ach.getSubCommand().type = SubCommand.UFTP;
		ach.submit();
		this.subactionID = ach.getActionID();
		do{
			try{
				checkCancelled();
			}catch(Exception ce){
				try{
					ach.abort();
				}catch(Exception e){}
				throw ce;
			}
			Thread.sleep(2000);
		}while(!ach.isDone());

		ResultHolder res = ach.getResult();
		if(res.getExitCode()==null || res.getExitCode()!=0){
			String message="UFTP data download failed.";
			try{
				String error = res.getErrorMessage();
				if(error!=null && error.length()>0)message+=" Error details: "+error;
			}catch(IOException ex){}
			throw new Exception(message);
		}
	}

	private void checkCancelled() {}

}