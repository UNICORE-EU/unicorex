package de.fzj.unicore.xnjs.tsi.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStagingInfo;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.util.ErrorCode;
import de.fzj.unicore.xnjs.util.UnitParser;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;

/**
 * Helper to
 * <ul>
 * <li> generate commands to the UNICORE TSI</li>
 * <li> deal with some replies such as status listings and directory listings</li>
 * </ul>
 * 
 * @author schuller
 */
public class TSIMessages {

	public static final String EXITCODE_FILENAME = "UNICORE_SCRIPT_EXIT_CODE";
	public static final String PID_FILENAME = "UNICORE_SCRIPT_PID";
	public static final String ALLOCATION_ID = "UNICORE_ALLOCATION_ID";

	private static final int MEGABYTE=1024*1024;

	static boolean  _unittestnoexitcode=false;

	private final XNJS xnjs;

	private final IOProperties ioProperties;

	public TSIMessages(XNJS xnjs) {
		this.xnjs = xnjs;
		this.ioProperties = xnjs.getIOProperties();
	}

	/**
	 * make a TSI "submit" command <br/>
	 *
	 * @return TSI commmand string
	 */
	public String makeSubmitCommand(Action job, String credentials) throws ExecutionException {
		ApplicationInfo applicationInfo = job.getApplicationInfo();
		Client client=job.getClient();
		ExecutionContext ec = job.getExecutionContext();
		IDB idb = xnjs.get(IDB.class);
		Incarnation grounder = xnjs.get(Incarnation.class);

		String template = idb.getSubmitTemplate()
				.replace("#COMMAND", "#TSI_SUBMIT\n");

		StringBuilder commands = new StringBuilder();
		Formatter f = new Formatter(commands, null);
				
		commands.append("\n"); // start on a fresh line independent of the script template

		List<ResourceRequest> rt=grounder.incarnateResources(job);

		ResourceRequest reservation=ResourceRequest.find(rt, ResourceSet.RESERVATION_ID);
		if(reservation!=null){
			rt.remove(reservation);
			String reservationRef = checkLegal(reservation.getRequestedValue(), "Reservation reference");
			if (reservationRef != null) {
				f.format("#TSI_RESERVATION_REFERENCE %s\n", reservationRef);
			}
		}
		if(credentials!=null){
			f.format("#TSI_CREDENTIALS %s\n", credentials);
		}
		if(applicationInfo.isAllocateOnly()) {
			f.format("#TSI_ALLOCATION_ID %s\n", TSIMessages.ALLOCATION_ID);
		}
		f.format("#TSI_OUTCOME_DIR %s\n", ec.getOutputDirectory());
		f.format("#TSI_USPACE_DIR %s\n", ec.getWorkingDirectory());
		String stdout=ec.getStdout()!=null? checkLegal(ec.getStdout(),"Stdout") : "stdout";
		f.format("#TSI_STDOUT %s\n", stdout);
		String stderr=ec.getStderr()!=null? checkLegal(ec.getStderr(),"Stderr") : "stderr";
		f.format("#TSI_STDERR %s\n", stderr);
		String jobName=job.getJobName()!=null ? checkLegal(job.getJobName(), "Job name") : "UNICORE_Job";
		f.format("#TSI_JOBNAME %s\n", jobName);
		String email="NONE";
		if(client!=null && client.getUserEmail()!=null){
			email = checkLegal(client.getUserEmail(),"Email");
		}
		f.format("#TSI_EMAIL %s\n", email);
		boolean normalMode = !applicationInfo.isRawJob();
		if(normalMode){
			String queue = appendTSIResourceSpec(commands, rt);
			ec.setBatchQueue(queue);
			job.setDirty();
			if(applicationInfo.isAllocateOnly()) {
				commands.append("#TSI_JOB_MODE allocate\n");
			}
		}
		else {
			String jobFile = applicationInfo.getRawBatchFile();
			commands.append("#TSI_JOB_MODE raw\n");
			f.format("#TSI_JOB_FILE %s\n", jobFile);
		}

		template = template.replace("#RESOURCES", commands.toString());
		f.close();

		commands = new StringBuilder();
		f = new Formatter(commands, null);
		// last bit to introduce script (extends to end of incarnation)
		commands.append("#TSI_SCRIPT\n");

		// add environment settings from context
		appendEnvironment(commands, ec, true);
		// Set User DN as env variable
		// useful e.g. if mapping multiple certs to single ulogin
		if (client!=null) {
			//just to be safe, make sure there are no quotes
			String dn=client.getDistinguishedName().replaceAll("\"", "_");
			f.format("UC_USERDN=\"%s\"; export UC_USERDN\n", dn);
		}
		
		// add "." to PATH to make sure user executable can be specified without
		// having to prefix "./"
		commands.append("PATH=$PATH:. ; export PATH\n");
		if (ec.getWorkingDirectory() != null) {
			f.format("UC_WORKING_DIRECTORY=\"%s\"; export UC_WORKING_DIRECTORY\n", ec.getWorkingDirectory());
		}
		if (ec.getOutputDirectory() != null) {
			f.format("UC_OUTPUT_DIRECTORY=\"%s\"; export UC_OUTPUT_DIRECTORY\n", ec.getOutputDirectory());
		}
		commands.append("cd ${UC_WORKING_DIRECTORY}\n");

		if(ioProperties.getBooleanValue(IOProperties.STAGING_FS_WAIT)) {
			// make sure all staged input files are available ON THE WORKER NODE
			insertImportedFilesWaitingLoop(commands, job);
		}

		// executable (user-pre, prologue, main, epilogue, user-post)
		insertExecutable(commands, applicationInfo, ec, false);
		f.close();
		return template.replace("#SCRIPT", commands.toString());
	}

	/**
	 * make sure all staged input files are available ON THE NODE where stuff will be running
	 */
	private void insertImportedFilesWaitingLoop(StringBuilder commands, Action job) {
		try
		{
			List<DataStageInInfo> stageIns = job.getStageIns();
			if(stageIns==null || stageIns.size()==0)return;
			int timeout = 20; //seconds
			StringBuffer fileList = new StringBuffer();
			for(DataStagingInfo dst:stageIns){
				try{
					fileList.append(" '");
					String fName=dst.getFileName();
					//strip separator
					while(fName.startsWith("/")){
						fName=fName.substring(1);
					}
					fName = checkLegal(fName, "File name");
					fileList.append(fName);
					fileList.append("'");
				}
				catch(Exception e){}
			}

			// wait for stage-ins to be visible on network file system
			commands.append("end=$((`date +%s`+").append(timeout).append("))\n");
			commands.append("_l=\"true\"\n");
			commands.append("while [ \"$_l\" = \"true\" ]\n");
			commands.append("do _l=\"false\"\n");
			commands.append("  for _f in ").append(fileList).append("\n");
			commands.append("  do if [ ! -e \"${_f}\" ]\n");
			commands.append("    then\n");
			commands.append("      _l=\"true\"\n");
			commands.append("      break\n");
			commands.append("    fi\n");
			commands.append("  done\n");
			commands.append("  current=`date +%s`\n");
			commands.append("  if [ $current -gt $end ]\n");
			commands.append("  then\n");
			// Waiting for input files to show up on the (shared) file system timed out
			commands.append("    break\n");
			commands.append("  fi\n");
			commands.append("  sleep 2\n");
			commands.append("done\n");

		}
		catch (Exception e) {}
	}

	private void insertExecutable(StringBuilder commands, ApplicationInfo applicationInfo, ExecutionContext ec,
			boolean redirectOutput) {
		Formatter f = new Formatter(commands);
		String executable = applicationInfo.getExecutable();
		boolean haveExecutable = executable!=null && executable.length()>0;
		if(haveExecutable) {
			executable = executable.trim();
			f.format("UC_EXECUTABLE=\"%s\"; export UC_EXECUTABLE\n", executable);
			//better guess the actual executable, by using the first part of
			//the executable line that is non-whitespace
			String executableGuess=executable;
			try{
				String[] tok=executable.split(" ");
				for(String t: tok){
					if(!t.trim().isEmpty()){
						executableGuess=t;
						break;
					}
				}
			}catch(Exception ex){}
			f.format("chmod u+x %s 2> /dev/null \n", executableGuess);

		}
		// remove any pre-existing exit code file (e.g. job restart case)
		if(!_unittestnoexitcode){
			f.format("rm -f ${UC_OUTPUT_DIRECTORY}/%s\n", ec.getExitCodeFileName());
		}

		String stdout = ec.isDiscardOutput()? "/dev/null" : "${UC_OUTPUT_DIRECTORY}/"+ec.getStdout();
		String stderr = ec.isDiscardOutput()? "/dev/null" : "${UC_OUTPUT_DIRECTORY}/"+ec.getStderr();
		String redirect = " >> "+stdout+" 2>> "+stderr;

		// user-defined pre-command
		String userPre = applicationInfo.getUserPreCommand();
		if(userPre != null && !applicationInfo.isUserPreCommandOnLoginNode()){
			commands.append("# user defined pre-command\n");
			commands.append(userPre);
			if(redirectOutput)commands.append(redirect);
			commands.append("\n");
		}

		// prologue (from IDB)
		if(applicationInfo.getPrologue()!=null) {
			commands.append(applicationInfo.getPrologue());
			if(redirectOutput)commands.append(redirect);
			commands.append("\n");
		}

		// setup executable
		if(haveExecutable) {
			StringBuilder exeBuilder = new StringBuilder();
			exeBuilder.append(applicationInfo.getExecutable());
			for (String a : applicationInfo.getArguments()) {
				exeBuilder.append(" ").append(a);
			}
			if(redirectOutput)exeBuilder.append(redirect);
			String input = null;
			input = ec.getStdin() != null ? ec.getStdin() : null;
			if (input != null) {
				exeBuilder.append(" < ${UC_WORKING_DIRECTORY}/").append(input);
			}
			commands.append(exeBuilder.toString()).append("\n");

			// write the application exit code to a special file
			commands.append("\n");
			if(!_unittestnoexitcode){
				f.format("echo $? > ${UC_OUTPUT_DIRECTORY}/%s\n", ec.getExitCodeFileName());
			}
		}
		
		// epilogue (from IDB)
		if(applicationInfo.getEpilogue()!=null) {
			commands.append(applicationInfo.getEpilogue());
			if(redirectOutput)commands.append(redirect);
			commands.append("\n");
		}

		// user-defined post-command
		String userPost = applicationInfo.getUserPostCommand();
		if(userPost != null && !applicationInfo.isUserPostCommandOnLoginNode()){
			commands.append("\n# user defined post-command\n");
			commands.append(userPost);
			if(redirectOutput)commands.append(redirect);
			commands.append("\n");
		}
		f.close();
	}
	
	/**
	 * generate an EXECUTE_SCRIPT command
	 */
	public String makeExecuteScript(String script, ExecutionContext ec, String credentials) {
		IDB idb = xnjs.get(IDB.class);
		String template = idb.getExecuteTemplate()
				.replace("#COMMAND", "#TSI_EXECUTESCRIPT\n");

		StringBuilder commands = new StringBuilder();
		Formatter f = new Formatter(commands, null);

		if(credentials!=null){
			f.format("#TSI_CREDENTIALS %s\n", credentials);
		}

		commands.append("#TSI_SCRIPT\n");

		if (ec != null) {
			appendEnvironment(commands, ec, true);
		}
		commands.append(script);
		if(ec!=null){
			if(ec.isDiscardOutput()){
				commands.append(" > /dev/null");
			}
			else if(ec.getStdout()!=null){
				commands.append(" > ").append(ec.getStdout());
			}
			if(ec.isDiscardOutput()){
				commands.append(" 2> /dev/null");
			}
			else if(ec.getStderr()!=null){
				commands.append(" 2> ").append(ec.getStderr());
			}	
			commands.append("\n");
			f.format("echo $? > %s/%s\n", ec.getWorkingDirectory(), ec.getExitCodeFileName());
		}
		f.close();
		return template.replace("#SCRIPT", commands.toString());
	}

	/**
	 * generate an EXECUTE_SCRIPT command for running a command asynchronously
	 */
	public String makeExecuteAsyncScript(Action job, String credentials) {
		IDB idb = xnjs.get(IDB.class);
		String template = idb.getExecuteTemplate()
				.replace("#COMMAND", "#TSI_EXECUTESCRIPT\n");
		ExecutionContext ec=job.getExecutionContext();
		ApplicationInfo ai=job.getApplicationInfo();
		ec.getEnvironment().putAll(ai.getEnvironment());
		StringBuilder commands = new StringBuilder();
		Formatter f = new Formatter(commands, null);

		if(credentials!=null){
			f.format("#TSI_CREDENTIALS %s\n", credentials);
		}
		commands.append("#TSI_DISCARD_OUTPUT true\n");
		commands.append("#TSI_SCRIPT\n");
		if (ec.getWorkingDirectory() != null) {
			f.format("UC_WORKING_DIRECTORY=%s; export UC_WORKING_DIRECTORY\n", ec.getWorkingDirectory());
		}
		if (ec.getOutputDirectory() != null) {
			f.format("UC_OUTPUT_DIRECTORY=%s; export UC_OUTPUT_DIRECTORY\n", ec.getOutputDirectory());
		}
		commands.append("cd ${UC_WORKING_DIRECTORY}\n");

		appendEnvironment(commands, ec, true);

		commands.append("{ ");
		
		if(ioProperties.getBooleanValue(IOProperties.STAGING_FS_WAIT)) {
			insertImportedFilesWaitingLoop(commands, job);
		}

		insertExecutable(commands, ai, ec, true);

		f.format("} & echo $! > ${UC_OUTPUT_DIRECTORY}/%s", ec.getPIDFileName());
		f.close();
		return template.replace("#SCRIPT", commands.toString());
	}

	private String [] ls_ignored = new String[] {"TSI_OK", "END_LISTING", "START_LISTING", "<", "-"};

	/**
	 * read a line from a TSI LS result, skipping irrelevant lines
	 * 
	 * @param br -reader to read from
	 * @return two lines of the directory listing
	 * @throws IllegalArgumentException if the result from TSI  makes no sense
	 */
	public String[] readTSILSLine(BufferedReader br) throws IllegalArgumentException {
		String lines[] = new String[2];
		lines[0] = "";
		try {
			nextline: while (lines[0] != null) {
				lines[0] = br.readLine();
				if (lines[0] == null)break;
				for(String i: ls_ignored) {
					if(lines[0].startsWith(i)) {
						continue nextline;
					}
				}
				if (lines[0].length() == 0)continue;
				lines[1] = br.readLine();
				if (!lines[1].startsWith("--")) {
					throw new IllegalArgumentException("Got invalid " +
							"extended permissions line from TSI: >" + 
							lines[1] + "< for file line >" 
							+ lines[0] + "<");
				}
				return lines;
			}
		}catch(IOException ioe) {
			// can't really happen but OK
			throw new IllegalStateException(ioe);
		}
		
		return lines;
	}

	private String [] df_ignored = new String[] {"TSI_OK", "END_DF", "START_DF"};

	public String readTSIDFLine(BufferedReader br) throws ExecutionException{
		String line = "";
		nextline: while (line != null) {
			try{
				line = br.readLine();
			}catch(IOException ex){
				throw new ExecutionException(ex);
			}
			if (line == null)break;
			for(String i: df_ignored) {
				if(line.startsWith(i)) {
					continue nextline;
				}
			}
			return line;
		}
		return null;
	}

	/**
	 * parse the allocation ID file produced by a job of type "allocate"
	 * @param tsiReply
	 * @return array containing the job ID and the BSS variable name for sending the job ID later
	 */
	public String[] readAllocationID(String tsiReply) {
		if(tsiReply==null || tsiReply.trim().length()==0) {
			throw new IllegalArgumentException("Empty "+ALLOCATION_ID+" file?");
		}
		String[]tokens = tsiReply.trim().split("\\n");
		String id = tokens[0];
		String jobIDVariableName = tokens.length>1 ?
				tokens[1] : "SLURM_JOB_ID";
		return new String[] {id, jobIDVariableName};
	}

	/**
	 * make an Abort command
	 * 
	 * @param bssid
	 * @return command string to send to the TSI
	 */
	public String makeAbortCommand(String bssid) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_ABORTJOB\n");
		commands.append("#TSI_BSSID ").append(bssid).append("\n");
		return commands.toString();
	}

	/**
	 * make a Cancel command
	 * 
	 * @param bssid
	 * @return command string to send to the TSI
	 */
	public String makeCancelCommand(String bssid) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_CANCELJOB\n");
		commands.append("#TSI_BSSID ").append(bssid).append("\n");
		return commands.toString();
	}


	/**
	 * make a "get job info" command
	 * 
	 * @param bssid
	 * @return command string to send to the TSI
	 */
	public String makeGetJobInfoCommand(String bssid, String credentials) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_GETJOBDETAILS\n");
		if(credentials!=null){
			commands.append("#TSI_CREDENTIALS ").append(credentials).append("\n");
		}
		commands.append("#TSI_BSSID ").append(bssid).append("\n");
		return commands.toString();
	}

	/**
	 * make a Get Status command
	 * 
	 * @return command string to send to the TSI
	 */
	public String makeStatusCommand(String credentials) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_GETSTATUSLISTING\n");
		if(credentials!=null){
			commands.append("#TSI_CREDENTIALS ").append(credentials).append("\n");
		}
		return commands.toString();
	}

	/**
	 * create a GetFileChunk command
	 * 
	 * @param file
	 * @param start
	 * @param length
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makeGetFileChunkCommand(String file, long start,
			long length) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_GETFILECHUNK\n");
		commands.append("#TSI_FILE " + file + "\n");
		commands.append("#TSI_START " + start + "\n");
		commands.append("#TSI_LENGTH " + length + "\n");
		return commands.toString();
	}

	/**
	 * create a PutFiles command
	 * 
	 * @param append
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makePutFilesCommand(boolean append) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_PUTFILES\n");
		String action = append ? "3" : "0";
		commands.append("#TSI_FILESACTION " + action + "\n");
		return commands.toString();
	}

	/**
	 * create a PutFileChunk command
	 * 
	 * @param append
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makePutFileChunkCommand(String file, String mode, long length, boolean append) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_PUTFILECHUNK\n");
		String action = append ? "3" : "0";
		commands.append("#TSI_FILESACTION " + action + "\n");
		commands.append("#TSI_FILE " + file + " " + mode + "\n");
		commands.append("#TSI_LENGTH " + length + "\n");
		return commands.toString();
	}

	public static String makeUFTPGetFileCommand(
			String host, int port, String secret,
			String remoteFile, 
			String localFile, 
			String workingDir, 
			long offset, long length, boolean partial, ExecutionContext ec) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_UFTP");
		commands.append("\n#TSI_UFTP_HOST ").append(host);
		commands.append("\n#TSI_UFTP_PORT ").append(port);
		commands.append("\n#TSI_UFTP_SECRET ").append(secret);
		commands.append("\n#TSI_UFTP_OPERATION GET");
		commands.append("\n#TSI_UFTP_REMOTE_FILE ").append(remoteFile);
		commands.append("\n#TSI_UFTP_LOCAL_FILE ").append(localFile);
		commands.append("\n#TSI_UFTP_WRITE_MODE ").append(partial ? "PARTIAL" : "FULL");
		commands.append("\n#TSI_UFTP_OFFSET ").append(offset);
		commands.append("\n#TSI_UFTP_LENGTH ").append(length);
		commands.append("\n#TSI_USPACE_DIR ").append(workingDir);
		commands.append("\n#TSI_OUTCOME_DIR ").append(ec.getOutputDirectory());
		commands.append("\n#TSI_STDOUT ").append(ec.getStdout());
		commands.append("\n#TSI_STDERR ").append(ec.getStderr());
		commands.append("\n#TSI_PID_FILE ").append(ec.getPIDFileName());
		commands.append("\n#TSI_EXIT_CODE_FILE ").append(ec.getExitCodeFileName());
		commands.append("\n");
		return commands.toString();
	}

	public static String makeUFTPPutFileCommand(
			String host, int port, String secret,
			String remoteFile, 
			String localFile, 
			String workingDir, 
			long offset, long length, boolean partial, ExecutionContext ec) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_UFTP");
		commands.append("\n#TSI_UFTP_HOST ").append(host);
		commands.append("\n#TSI_UFTP_PORT ").append(port);
		commands.append("\n#TSI_UFTP_SECRET ").append(secret);
		commands.append("\n#TSI_UFTP_OPERATION PUT");
		commands.append("\n#TSI_UFTP_REMOTE_FILE ").append(remoteFile);
		commands.append("\n#TSI_UFTP_LOCAL_FILE ").append(localFile);
		commands.append("\n#TSI_UFTP_WRITE_MODE ").append(partial ? "PARTIAL" : "FULL");
		commands.append("\n#TSI_UFTP_OFFSET ").append(offset);
		commands.append("\n#TSI_UFTP_LENGTH ").append(length);
		commands.append("\n#TSI_USPACE_DIR ").append(workingDir);
		commands.append("\n#TSI_OUTCOME_DIR ").append(ec.getOutputDirectory());
		commands.append("\n#TSI_STDOUT ").append(ec.getStdout());
		commands.append("\n#TSI_PID_FILE ").append(ec.getPIDFileName());
		commands.append("\n#TSI_EXIT_CODE_FILE ").append(ec.getExitCodeFileName());
		commands.append("\n");
		return commands.toString();
	}


	/**
	 * Build the command to the UNICORE TSI for making a resource reservation.
	 * 
	 * @param rt -
	 *            the resources to reserve
	 * @param startTime -
	 *            the requested start time
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makeMakeReservationCommand(List<ResourceRequest>rt, Calendar startTime, Client client) {
		StringBuilder commands = new StringBuilder();
		commands.append("#TSI_MAKE_RESERVATION\n");
		if(client!=null && client.getXlogin()!=null){
			commands.append("#TSI_RESERVATION_OWNER " + client.getXlogin().getUserName() + "\n");
		}
		DateFormat df = UnitParser.getISO8601();
		commands.append("#TSI_STARTTIME " + df.format(startTime.getTime()) + "\n");
		appendTSIResourceSpec(commands, rt);
		return commands.toString();
	}

	/**
	 * Build the command to the UNICORE TSI for cancelling a resource
	 * reservation
	 * 
	 * @param reservationID - the ID of the reservation to cancel
	 */
	public String makeCancelReservationCommand(String reservationID) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_CANCEL_RESERVATION\n");
		commands.append("#TSI_RESERVATION_REFERENCE " + reservationID + "\n");
		return commands.toString();
	}

	/**
	 * Build the command to the UNICORE TSI for querying the status of a
	 * resource reservation
	 * 
	 * @param reservationID -
	 *            the ID of the reservation to query
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makeQueryReservationCommand(String reservationID) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_QUERY_RESERVATION\n");
		commands.append("#TSI_RESERVATION_REFERENCE " + reservationID + "\n");
		return commands.toString();
	}

	/**
	 * Build the command to the UNICORE TSI for listing a file/directory
	 * 
	 * @param path - the file/directory to stat or list
	 * @param normal - normal directory listing or stat dir as a single file
	 * @param recurse - recurse in case of a directory listing 
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makeLSCommand(String path, boolean normal, boolean recurse) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_LS\n");
		String mode = normal? (recurse ? "R" : "N"): "A" ;
		commands.append("#TSI_LS_MODE ").append(mode).append("\n");
		commands.append("#TSI_FILE ").append(path).append("\n");
		return commands.toString();
	}

	/**
	 * Build the command to the UNICORE TSI for getting free disk space
	 * 
	 * @param path - the file/directory to stat or list
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makeDFCommand(String path) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_DF\n");
		commands.append("#TSI_FILE ").append(path).append("\n");
		return commands.toString();
	}

	/**
	 * Build the command to the UNICORE TSI for getting the remaining 
	 * compute budget for the current user
	 * 
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makeGetBudgetCommand() {
		return "#TSI_GET_COMPUTE_BUDGET\n";
	}

	/**
	 * Build the command to the UNICORE TSI for getting
	 * the process list on the TSI node
	 *
	 * @return a string for sending to the UNICORE TSI
	 */
	public String makeGetProcessListCommand() {
		return "#TSI_GETPROCESSLISTING\n";
	}

	/**
	 * builds the command required to get user's ssh public keys
	 * @return
	 */
	public String makeGetUserInfoCommand() {
		return "#TSI_GET_USER_INFO\n";
	}
	
	/**
	 * Extract the resources from the <code>&lt;ResourceSet&gt;</code>
	 * and put them into #TSI_NNN strings<br/>
	 * 
	 * @return the queue / partition name the job will be submitted into
	 */
	public String appendTSIResourceSpec(StringBuilder commands, List<ResourceRequest> resources) {

		int run_time = -1; // walltime

		int nodes = -1;
		int total_processors = -1;
		int processors_per_node = -1;

		int memory = -1;

		String queue="NONE";

		Formatter f = new Formatter(commands, null);
		//check Queue resource
		ResourceRequest queueResource = ResourceRequest.findAndRemove(resources,ResourceSet.QUEUE);
		if(queueResource!=null){
			queue = checkLegal(queueResource.getRequestedValue(),"Queue");
		}

		// project
		ResourceRequest projectResource = ResourceRequest.findAndRemove(resources,ResourceSet.PROJECT);
		if(projectResource!=null){
			String project = checkLegal(projectResource.getRequestedValue(),"Project");
			if(project!=null){
				f.format("#TSI_PROJECT %s\n", project);
			}
		}
		
		// quality of service
		ResourceRequest qosResource = ResourceRequest.findAndRemove(resources, ResourceSet.QOS);
		if(qosResource!=null){
			String qos = checkLegal(qosResource.getRequestedValue(), "QoS");
			f.format("#TSI_QOS %s\n", qos);
		}
		
		// node constraints
		ResourceRequest nodeConstraints = ResourceRequest.findAndRemove(resources, ResourceSet.NODE_CONSTRAINTS);
		if(nodeConstraints!=null){
			String nc=checkLegal(nodeConstraints.getRequestedValue(),"Node constraints");
			if(nc!=null){
				f.format("#TSI_BSS_NODES_FILTER %s\n", nc);
			}
		}

		// job array size and limit
		ResourceRequest arraySizeResource = ResourceRequest.findAndRemove(resources, ResourceSet.ARRAY_SIZE);
		if(arraySizeResource!=null){
			ResourceRequest arrayLimitResource = null;
			int size = Integer.parseInt(arraySizeResource.getRequestedValue())-1;
			if(size>0){
				f.format("#TSI_ARRAY 0-%d\n", size);
				arrayLimitResource = ResourceRequest.findAndRemove(resources, ResourceSet.ARRAY_LIMIT);
				if(arrayLimitResource!=null){
					f.format("#TSI_ARRAY_LIMIT %s\n", arrayLimitResource.getRequestedValue());
				}
			}
		}

		// CPUs / nodes
		ResourceRequest totalCpusRequest=ResourceRequest.findAndRemove(resources, ResourceSet.TOTAL_CPUS);
		if (totalCpusRequest!= null) {
			try {
				total_processors = Integer.valueOf(totalCpusRequest.getRequestedValue());
			} catch (RuntimeException e) {}
		}
		ResourceRequest cpusRequest=ResourceRequest.findAndRemove(resources, ResourceSet.CPUS_PER_NODE);
		if(cpusRequest!=null){
			try {
				processors_per_node = Integer.valueOf(cpusRequest.getRequestedValue());
			} catch (RuntimeException e) {}
		}
		ResourceRequest nodesRequest=ResourceRequest.findAndRemove(resources, ResourceSet.NODES);
		if (nodesRequest!= null) {
			try {
				nodes = Integer.valueOf(nodesRequest.getRequestedValue());
			} catch (RuntimeException e) {}
		}

		// memory per node
		ResourceRequest memoryRequest=ResourceRequest.findAndRemove(resources, ResourceSet.MEMORY_PER_NODE);
		if (memoryRequest!= null) {
			try {
				memory = Integer.valueOf(memoryRequest.getRequestedValue()) / MEGABYTE;
			} catch (RuntimeException e) {}
		}

		// time, individual == wall clock time
		run_time = getRuntime(resources);
		ResourceRequest.removeQuietly(resources, ResourceSet.RUN_TIME);

		//total number of processors
		//int total=nodes!=-1? nodes*processors: processors;

		if(run_time>0){
			f.format("#TSI_TIME %d\n", run_time);
		}

		if(memory>0){
			f.format("#TSI_MEMORY %d\n", memory);
		}

		// can also have nodes not set at all (TSI does the mapping based on
		// total number of processors)
		f.format("#TSI_NODES %d\n", nodes);
		f.format("#TSI_PROCESSORS_PER_NODE %d\n", processors_per_node);
		f.format("#TSI_TOTAL_PROCESSORS %d\n", total_processors);
		f.format("#TSI_QUEUE %s\n", queue);
		
		// Set some resources as environment variables
		f.format("UC_NODES=%d; export UC_NODES\n", nodes);
		f.format("UC_PROCESSORS_PER_NODE=%d; export UC_PROCESSORS_PER_NODE\n", processors_per_node);
		f.format("UC_TOTAL_PROCESSORS=%d; export UC_TOTAL_PROCESSORS\n", total_processors);
		f.format("UC_RUNTIME=%d; export UC_RUNTIME\n", run_time);
		f.format("UC_MEMORY_PER_NODE=%d; export UC_MEMORY_PER_NODE\n", memory);

		f.close();

		appendSiteSpecificResources(commands, resources);

		return queue;
	}

	static List<String> filteredVariables = new ArrayList<>();
	static{
		filteredVariables.add("UC_OAUTH_BEARER_TOKEN");
	}

	/**
	 * Appends the environment to the commands argument in the 'key="value"; export key' format.
	 * Also appends umask setting if is set.
	 */
	private void appendEnvironment(StringBuilder commands,
			ExecutionContext ec, boolean filter) {
		Formatter f = new Formatter(commands, null);
		if (ec.getUmask() != null) {
			f.format("#TSI_UMASK %s\n", ec.getUmask());
			f.format("umask %s\n", ec.getUmask());
		}
		for (Map.Entry<String, String> env : ec.getEnvironment().entrySet()) {
			String key = env.getKey();
			if(filter&&filteredVariables.contains(key)){
				continue;
			}
			f.format("%s=\"%s\"; export %s\n", key, env.getValue(), key);
		}
		f.close();
	}

	protected static void appendSiteSpecificResources(StringBuilder commands,
			List<ResourceRequest> rs) {
		try (Formatter f = new Formatter(commands, null)){
			for (ResourceRequest r : rs) {
				try {
					String name = r.getName().replace(' ', '_');
					String value = checkLegal(r.getRequestedValue(),name);
					f.format("#TSI_SSR_%s %s\n", name.toUpperCase(), value);
				} catch (Exception e) {}
			}
		} catch (Exception e) {}
	}

	
	/**
	 * Prepares a standard groups string for a TSI call requesting membership in all groups configured
	 * by attribute sources and possibly refined by user preferences.
	 * @param client
	 */
	public static String prepareGroupsString(Client client) {
		Xlogin xlogin = client.getXlogin();
		StringBuilder sb = new StringBuilder();
		String group = xlogin.getGroup();

		//means that neither primary nor supplementary gids were defined 
		if (group == null || group.length()==0) {
			if (xlogin.isAddDefaultGroups()) {
				return "DEFAULT_GID";
			}
			else return "NONE";
		}

		//We got something. If only supplementary groups then use OS default not the first supplementary.
		if (xlogin.isGroupSelected())
			sb.append(group);
		else
			sb.append("DEFAULT_GID");

		String supGids = xlogin.getEncodedSelectedSupplementaryGroups();
		if (!supGids.equals("")) {
			sb.append(":");
			sb.append(supGids);
		}

		if (xlogin.isAddDefaultGroups())
			sb.append(":DEFAULT_GID");
		return sb.toString();

	}

	/**
	 * Prepares a groups string for a TSI call requesting membership in all groups allowed for the user
	 * in attribute sources + all default OS groups.
	 * @param client
	 */
	public static String prepareAllGroupsString(Client client) {
		Xlogin xlogin = client.getXlogin();
		StringBuilder sb = new StringBuilder();

		sb.append("DEFAULT_GID");

		//add all allowed groups
		if (xlogin.getEncodedGroups() != null && xlogin.getEncodedGroups().length() > 0)
			sb.append(":").append(xlogin.getEncodedGroups());

		//get all OS supplementary groups too
		sb.append(":DEFAULT_GID");
		return sb.toString();
	}

	public String makePauseCommand(String bssid) {
		final StringBuffer commands = new StringBuffer();
		commands.append("#TSI_HOLDJOB\n");
		commands.append("#TSI_BSSID " + bssid + "\n");
		return commands.toString();
	}

	public String makeResumeCommand(String bssid) {
		final StringBuffer commands = new StringBuffer();
		commands.append("#TSI_RESUMEJOB\n");
		commands.append("#TSI_BSSID " + bssid + "\n");
		return commands.toString();
	}

	public String getAbortProcessCommand(String pid) {
		return xnjs.get(TSIProperties.class).getValue(TSIProperties.TSI_KILL).replace("[PID]", pid);
	}


	public static String getFilePerm(Integer umask) {
		return Integer.toOctalString(TSI.DEFAULT_FILE_PERMS & ~umask); 
	}

	public static String getDirPerm(Integer umask) {
		return Integer.toOctalString(TSI.DEFAULT_DIR_PERMS & ~umask); 
	}

	/**
	 * get the run time (in seconds) from the requested resource, 
	 * or <code>-1</code> if not set
	 * @param rt - the resource set
	 */
	public static int getRuntime(List<ResourceRequest> rt){
		int runtime = -1;
		ResourceRequest timeReq = ResourceRequest.find(rt, ResourceSet.RUN_TIME);
		if (timeReq!= null) {
			try {
				runtime = Integer.valueOf(timeReq.getRequestedValue());
			} catch (RuntimeException e) {}
		}
		return runtime;
	}


	private static String[] illegal = {"\n","\r","`","$("};

	/**
	 * Check if the input string contains illegal characters
	 *
	 * @param input - the input string coming from an untrusted source
	 * @param desc - a description of the input
	 * @return the string itself if it is OK
	 */
	public static String checkLegal(String input, String desc) throws IllegalArgumentException{
		if(input == null)return null;
		if(desc==null)desc="Input";

		for(String exp : illegal){
			if(input.contains(exp)){
				throw new IllegalArgumentException(desc+" contains illegal characters: '"+exp+"'");
			}
		}
		return input;
	}


	/**
	 * sanitize the input string - check for single quote (') characters that might mess
	 * up the script sent to the TSI
	 * 
	 * @param input - the input string coming from an untrusted source
	 * @return sanitized string with single quotes replaced by a properly quoted version
	 */
	public static String sanitize(String input){
		String sane = input.replace("'","'\\''");
		return sane;
	}

	/**
	 * check if TSI replied ONLY with TSI_OK
	 * @param reply
	 * @param tsiHost
	 * @throws ExecutionException
	 */
	public static void checkNoErrors(String reply, String tsiHost) throws ExecutionException {
		if (reply==null || !reply.startsWith("TSI_OK")
			||  reply.replaceFirst("TSI_OK", "").trim().length()>0) {
			String error = reply!=null? reply.replace("TSI_OK", "").trim() : "TSI reply is null";
			throw new ExecutionException(ErrorCode.ERR_TSI_EXECUTION, "TSI <"+tsiHost+"> ERROR: '"+error+"'");
		}
	}

}
