/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/

package de.fzj.unicore.xnjs.tsi.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;

import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStagingInfo;
import de.fzj.unicore.xnjs.jsdl.JSDLResourceSet;
import de.fzj.unicore.xnjs.jsdl.JSDLUtils;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;

/**
 * Helpers to
 * <ul>
 * <li> generate commands to the classic TSI</li>
 * <li> deal with some replies such as status listings and directory listings</li>
 * </ul>
 * 
 * Also holds the code that creates a resource description for the classic TSI
 * from a JSDL resource element,
 * 
 * @author schuller
 */
public class TSIUtils {

	public static final String EXITCODE_FILENAME = "UNICORE_SCRIPT_EXIT_CODE";
	public static final String PID_FILENAME = "UNICORE_SCRIPT_PID";

	private static final String TEMPLATE_COMMAND = "#COMMAND";
	private static final String TEMPLATE_RESOURCES = "#RESOURCES";
	private static final String TEMPLATE_SCRIPT = "#SCRIPT";

	public static final String DEFAULT_EXECUTE_TEMPLATE = "#!/bin/bash \n"
			+ TEMPLATE_COMMAND + "\n"
			+ TEMPLATE_SCRIPT + "\n";

	public static final String DEFAULT_SUBMIT_TEMPLATE = "#!/bin/bash \n"
			+ TEMPLATE_COMMAND + "\n" + TEMPLATE_RESOURCES + "\n"
			+ TEMPLATE_SCRIPT + "\n";

	private static final int MEGABYTE=1024*1024;

	static boolean  _unittestnoexitcode=false;

	private TSIUtils() {
	}

	/**
	 * make a TSI "submit" command <br/>
	 *
	 * @return TSI commmand string
	 */
	public static String makeSubmitCommand(Action job, 
			IDB idb, Incarnation grounder, 
			XNJSProperties properties, String credentials, boolean addWaitingLoop) throws ExecutionException {
		ApplicationInfo applicationInfo=job.getApplicationInfo();
		Client client=job.getClient();
		ExecutionContext ec = job.getExecutionContext();

		String template = idb.getSubmitTemplate();

		List<ResourceRequest> rt=grounder.incarnateResources(job);

		if (template == null)
			template = DEFAULT_SUBMIT_TEMPLATE;

		template = template.replace(TEMPLATE_COMMAND, "#TSI_SUBMIT\n");

		// make the resources
		StringBuilder commands = new StringBuilder();
		commands.append("\n"); // start on a fresh line independent of the script template
		// resource booking reference
		ResourceRequest reservation=ResourceRequest.find(rt, ResourceSet.RESERVATION_ID);
		if(reservation!=null){
			rt.remove(reservation);
			String reservationRef = checkLegal(reservation.getRequestedValue(), "Reservation reference");
			if (reservationRef != null) {
				commands.append("#TSI_RESERVATION_REFERENCE " + reservationRef
						+ "\n");
			}
		}
		if(credentials!=null){
			commands.append("#TSI_CREDENTIALS " + credentials + "\n");
		}

		commands.append("#TSI_OUTCOME_DIR " + ec.getOutcomeDirectory() + "\n");
		commands.append("#TSI_USPACE_DIR " + ec.getWorkingDirectory() + "\n");

		String stdout=ec.getStdout()!=null? checkLegal(ec.getStdout(),"Stdout") : "stdout";
		commands.append("#TSI_STDOUT "+stdout+"\n");
		String stderr=ec.getStderr()!=null? checkLegal(ec.getStderr(),"Stderr") : "stderr";
		commands.append("#TSI_STDERR "+stderr+"\n");

		String jobName=job.getJobName()!=null ? checkLegal(job.getJobName(), "Job name") : "UNICORE_Job";
		commands.append("#TSI_JOBNAME "+jobName+"\n");
		String email="NONE";
		if(client!=null && client.getUserEmail()!=null){
			email = checkLegal(client.getUserEmail(),"Email");
		}
		commands.append("#TSI_EMAIL " + email + "\n");

		boolean normalMode = !applicationInfo.isRawJob();

		if(normalMode){
			String queue = appendTSIResourceSpec(commands, rt);
			ec.setBatchQueue(queue);
			job.setDirty();
		}
		else {
			String jobFile = applicationInfo.getRawBatchFile();
			commands.append("#TSI_JOB_MODE raw\n");
			commands.append("#TSI_JOB_FILE "+jobFile+"\n");
		}

		template = template.replace(TEMPLATE_RESOURCES, commands.toString());

		commands = new StringBuilder();
		// last bit to introduce script (extends to end of incarnation)
		commands.append("#TSI_SCRIPT\n");

		// add environment settings from context
		appendEnvironment(commands, ec, true);

		// add XNJS environment variables UC_XXXX

		// executable
		String executable=applicationInfo.getExecutable();
		commands.append("UC_EXECUTABLE='"+executable+"'; export UC_EXECUTABLE\n");

		// Set User DN as env variable
		// useful e.g. if mapping multiple certs to single ulogin
		if (client!=null) {
			//just to be safe, make sure there are no quotes
			String dn=client.getDistinguishedName().replaceAll("\"", "_");
			commands.append("UC_USERDN=\"" + dn + "\"; export UC_USERDN\n");
		}

		// add "." to PATH to make sure user executable can be specified without
		// having to prefix "./"
		commands.append("PATH=$PATH:. ; export PATH\n");

		// chdir to working dir
		commands.append("cd " + ec.getWorkingDirectory() + "\n");

		if(addWaitingLoop) {
			// make sure all staged input files are available ON THE WORKER NODE
			insertImportedFilesWaitingLoop(commands, job);
		}
		
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

		commands.append("chmod u+x " + executableGuess + " 2> /dev/null \n");

		// remove any pre-existing exit code file (e.g. job restart case)
		if(!_unittestnoexitcode){
			commands.append("rm -f " + ec.getOutcomeDirectory() + "/"
					+ ec.getExitCodeFileName() + "\n");
		}

		// user-defined pre-command
		String userPre = applicationInfo.getUserPreCommand();
		if(userPre != null && !applicationInfo.isUserPreCommandOnLoginNode()){
			commands.append("# user defined pre-command\n");
			commands.append(userPre + "\n");
		}

		// prologue (from IDB)
		if(applicationInfo.getPrologue()!=null) {
			commands.append(applicationInfo.getPrologue()).append("\n");
		}

		// setup executable

		StringBuilder exeBuilder=new StringBuilder();
		exeBuilder.append(applicationInfo.getExecutable());

		for (String a : applicationInfo.getArguments()) {
			exeBuilder.append(" " + a);
		}

		String input = null;
		input = ec.getStdin() != null ? ec.getStdin() : null;
		if (input != null) {
			exeBuilder.append(" < ").append(input);
		}
		commands.append(exeBuilder.toString()).append("\n");

		// write the application exit code to a special file
		commands.append("\n");
		if(!_unittestnoexitcode){
			commands.append("echo $? > " + ec.getOutcomeDirectory() + "/"
					+ ec.getExitCodeFileName() + "\n");
		}

		// epilogue (from IDB)
		if(applicationInfo.getEpilogue()!=null) {
			commands.append(applicationInfo.getEpilogue()).append("\n");
		}


		// user-defined post-command
		String userPost = applicationInfo.getUserPostCommand();
		if(userPost != null && !applicationInfo.isUserPostCommandOnLoginNode()){
			commands.append("\n# user defined post-command\n");
			commands.append(userPost).append("\n");
		}

		return template.replace(TEMPLATE_SCRIPT, commands.toString());
	}

	/**
	 * make sure all staged input files are available ON THE NODE where stuff will be running
	 */
	public static void insertImportedFilesWaitingLoop(StringBuilder commands, Action job) {
		try
		{
			List<DataStageInInfo> stageIns = job.getStageIns();
			if(stageIns.size()==0)return;
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
	
	/**
	 * generate an EXECUTE_SCRIPT command
	 */
	public static String makeExecuteScript(String script, ExecutionContext ec,
			IDB idb, String credentials) {
		String template = idb.getExecuteTemplate();
		if (template == null)
			template = DEFAULT_EXECUTE_TEMPLATE;

		template = template.replace(TEMPLATE_COMMAND, "#TSI_EXECUTESCRIPT\n");

		StringBuilder commands = new StringBuilder();
		if(credentials!=null){
			commands.append("#TSI_CREDENTIALS " + credentials + "\n");
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
				commands.append(" > "+ec.getStdout());
			}
			if(ec.isDiscardOutput()){
				commands.append(" 2> /dev/null");
			}
			else if(ec.getStderr()!=null){
				commands.append(" 2> "+ec.getStderr());
			}	
		}
		commands.append("\n");

		if (ec != null) {
			commands.append("echo $? > ").append(ec.getWorkingDirectory()).append("/")
			.append(ec.getExitCodeFileName()).append("\n");
		}

		return template.replace(TEMPLATE_SCRIPT, commands.toString());
	}

	/**
	 * generate an EXECUTE_SCRIPT command for running a command asynchronously
	 */
	public static String makeExecuteAsyncScript(Action job,	IDB idb, String credentials, boolean waitingLoop) {

		String template = idb.getExecuteTemplate();
		if (template == null)
			template = DEFAULT_EXECUTE_TEMPLATE;

		template = template.replace(TEMPLATE_COMMAND, "#TSI_EXECUTESCRIPT\n");
		ExecutionContext ec=job.getExecutionContext();
		ApplicationInfo ai=job.getApplicationInfo();
		ec.getEnvironment().putAll(ai.getEnvironment());
		StringBuilder commands = new StringBuilder();
		if(credentials!=null){
			commands.append("#TSI_CREDENTIALS " + credentials + "\n");
		}
		commands.append("#TSI_DISCARD_OUTPUT true\n");
		commands.append("#TSI_SCRIPT\n");

		appendEnvironment(commands, ec, true);

		commands.append("cd " + ec.getWorkingDirectory() + "\n");

		commands.append(" { ");
		
		if(waitingLoop) insertImportedFilesWaitingLoop(commands, job);

		commands.append(ai.getExecutable());
		for(String arg: ai.getArguments()){
			commands.append(" ").append(arg);
		}
		commands.append(" > ");
		if(ec.isDiscardOutput()){
			commands.append("/dev/null");
		}
		else{
			commands.append(ec.getOutcomeDirectory()).append(ec.getStdout());
		}

		commands.append(" 2> ");
		if(ec.isDiscardOutput()){
			commands.append("/dev/null");
		}
		else{
			commands.append(ec.getOutcomeDirectory()).append(ec.getStderr());
		}

		commands.append("; echo $? > ").append(ec.getOutcomeDirectory()).append("/");
		commands.append(ec.getExitCodeFileName());
		commands.append(" ; } & ");
		commands.append("echo $! > ").append(ec.getOutcomeDirectory()).append("/");
		commands.append(ec.getPIDFileName());
		return template.replace(TEMPLATE_SCRIPT, commands.toString());
	}


	/**
	 * read a line from a TSI LS result, skipping irrelevant lines
	 * 
	 * @param br -reader to read from
	 * @return two lines of the directory listing
	 * @throws IllegalArgumentException if the result from TSI  makes no sense
	 */
	public static String[] readTSILSLine(BufferedReader br) throws IllegalArgumentException {
		String lines[] = new String[2];
		lines[0] = "";
		try {
			while (lines[0] != null) {
				lines[0] = br.readLine();
				if (lines[0] == null)
					break;
				// ignore lines with special meaning
				if (lines[0].startsWith("<"))
					continue;
				if (lines[0].startsWith("-"))
					continue;
				if (lines[0].startsWith("TSI_OK"))
					continue;
				if (lines[0].startsWith("END_LISTING"))
					continue;
				if (lines[0].startsWith("START_LISTING"))
					continue;
				if (lines[0].length() == 0)
					continue;

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

	public static String readTSIDFLine(BufferedReader br) throws ExecutionException{
		String line = "";
		while (line != null) {
			try{
				line = br.readLine();
			}catch(IOException ex){
				throw new ExecutionException(ex);
			}
			if (line == null)
				break;
			// ignore lines with special meaning to xNJS
			if (line.startsWith("TSI_OK"))
				continue;
			if (line.startsWith("END_DF"))
				continue;
			if (line.startsWith("START_DF"))
				continue;
			if (line.length() == 0)
				continue;

			return line;
		}
		return null;
	}

	/**
	 * make an Abort command
	 * 
	 * @param bssid
	 * @return command string to send to the TSI
	 */
	public static String makeAbortCommand(String bssid) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_ABORTJOB\n");
		commands.append("#TSI_BSSID " + bssid + "\n");
		return commands.toString();
	}

	/**
	 * make a Cancel command
	 * 
	 * @param bssid
	 * @return command string to send to the TSI
	 */
	public static String makeCancelCommand(String bssid) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_CANCELJOB\n");
		commands.append("#TSI_BSSID " + bssid + "\n");
		return commands.toString();
	}


	/**
	 * make a "get job info" command
	 * 
	 * @param bssid
	 * @return command string to send to the TSI
	 */
	public static String makeGetJobInfoCommand(String bssid, String credentials) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_GETJOBDETAILS\n");
		if(credentials!=null){
			commands.append("#TSI_CREDENTIALS " + credentials + "\n");
		}
		commands.append("#TSI_BSSID " + bssid + "\n");
		return commands.toString();
	}

	/**
	 * make a Get Status command
	 * 
	 * @return command string to send to the TSI
	 */
	public static String makeStatusCommand(String credentials) {
		StringBuffer commands = new StringBuffer();
		if(credentials!=null){
			commands.append("#TSI_CREDENTIALS " + credentials + "\n");
		}
		commands.append("#TSI_GETSTATUSLISTING\n");
		return commands.toString();
	}

	/**
	 * create a GetFileChunk command
	 * 
	 * @param file
	 * @param start
	 * @param length
	 * @return a string for sending to the classic TSI
	 */
	public static String makeGetFileChunkCommand(String file, long start,
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
	 * @return a string for sending to the classic TSI
	 */
	public static String makePutFilesCommand(boolean append) {
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
	 * @return a string for sending to the classic TSI
	 */
	public static String makePutFileChunkCommand(String file, String mode, long length, boolean append) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_PUTFILECHUNK\n");

		String action = append ? "3" : "0";
		commands.append("#TSI_FILESACTION " + action + "\n");
		commands.append("#TSI_FILE " + file + " " + mode + "\n");
		commands.append("#TSI_LENGTH " + length + "\n");
		return commands.toString();
	}

	/**
	 * If given, extract the reservation ID from the JSDL Resources element<br/>
	 * It is assumed to be in an element<br/> &lt;u:ReservationReference
	 * xmlns:u="http://www.unicore.eu/unicore/xnjs"&gt;...&lt;u:ReservationReference&gt;
	 * 
	 * @param rt -
	 *            Resources
	 * @return A reservation ID
	 */
	public static String extractReservationID(ResourcesType rt) {
		XmlObject[] xo = rt.selectChildren(JSDLResourceSet.RESERVATION_REFERENCE);
		if (xo == null || xo.length != 1) {
			return null;
		}
		XmlCursor c = xo[0].newCursor();
		String id = c.getTextValue();
		c.dispose();
		return id;
	}

	/**
	 * Build the command to the classic TSI for making a resource reservation.
	 * 
	 * @param rt -
	 *            the resources to reserve
	 * @param startTime -
	 *            the requested start time
	 * @return a string for sending to the classic TSI
	 */
	public static String makeMakeReservationCommand(List<ResourceRequest>rt, Calendar startTime, Client client) {
		StringBuilder commands = new StringBuilder();
		commands.append("#TSI_MAKE_RESERVATION\n");
		if(client!=null && client.getXlogin()!=null){
			commands.append("#TSI_RESERVATION_OWNER " + client.getXlogin().getUserName() + "\n");
		}
		DateFormat df = JSDLUtils.getDateFormat();
		commands.append("#TSI_STARTTIME " + df.format(startTime.getTime())
		+ "\n");
		// insert resource spec
		appendTSIResourceSpec(commands, rt);
		return commands.toString();
	}

	/**
	 * Build the command to the classic TSI for cancelling a resource
	 * reservation
	 * 
	 * @param reservationID - the ID of the reservation to cancel
	 */
	public static String makeCancelReservationCommand(String reservationID) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_CANCEL_RESERVATION\n");
		commands.append("#TSI_RESERVATION_REFERENCE " + reservationID + "\n");
		return commands.toString();
	}

	/**
	 * Build the command to the classic TSI for querying the status of a
	 * resource reservation
	 * 
	 * @param reservationID -
	 *            the ID of the reservation to query
	 * @return a string for sending to the classic TSI
	 */
	public static String makeQueryReservationCommand(String reservationID) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_QUERY_RESERVATION\n");
		commands.append("#TSI_RESERVATION_REFERENCE " + reservationID + "\n");
		return commands.toString();
	}

	/**
	 * Build the command to the classic TSI for listing a file/directory
	 * 
	 * @param path - the file/directory to stat or list
	 * @param normal - normal directory listing or stat dir as a single file
	 * @param recurse - recurse in case of a directory listing 
	 * @return a string for sending to the classic TSI
	 */
	public static String makeLSCommand(String path, boolean normal, boolean recurse) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_LS\n");
		String mode = normal? (recurse ? "R" : "N"): "A" ;
		commands.append("#TSI_LS_MODE ").append(mode).append("\n");
		commands.append("#TSI_FILE ").append(path).append("\n");
		return commands.toString();
	}

	/**
	 * Build the command to the classic TSI for getting free disk space
	 * 
	 * @param path - the file/directory to stat or list
	 * @return a string for sending to the classic TSI
	 */
	public static String makeDFCommand(String path) {
		StringBuffer commands = new StringBuffer();
		commands.append("#TSI_DF\n");
		commands.append("#TSI_FILE ").append(path).append("\n");
		return commands.toString();
	}

	/**
	 * Build the command to the classic TSI for getting the remaining 
	 * compute budget for the current user
	 * 
	 * @return a string for sending to the classic TSI
	 */
	public static String makeGetBudgetCommand() {
		return "#TSI_GET_COMPUTE_BUDGET\n";
	}

	/**
	 * Extract the resources from the <code>&lt;ResourceSet&gt;</code>
	 * and put them into #TSI_NNN strings<br/>
	 * 
	 * @return the queue / partition name the job will be submitted into
	 */
	public static String appendTSIResourceSpec(StringBuilder commands, List<ResourceRequest> resources) {

		int run_time = -1; // walltime

		int nodes = -1;
		int total_processors = -1;
		int processors_per_node = -1;

		int memory = -1;

		String queue="NONE";

		//check Queue resource
		ResourceRequest queueResource=ResourceRequest.find(resources,ResourceSet.QUEUE);
		if(queueResource!=null){
			queue=checkLegal(queueResource.getRequestedValue(),"Queue");
			resources.remove(queueResource);
		}

		// project
		ResourceRequest projectResource=ResourceRequest.find(resources,ResourceSet.PROJECT);
		if(projectResource!=null){
			String project=checkLegal(projectResource.getRequestedValue(),"Project");
			if(project!=null){
				commands.append("#TSI_PROJECT " + project + "\n");
			}
			resources.remove(projectResource);
		}
		
		// quality of service
		ResourceRequest qosResource=ResourceRequest.find(resources, ResourceSet.QOS);
		if(qosResource!=null){
			String qos = checkLegal(qosResource.getRequestedValue(), "QoS");
			commands.append("#TSI_QOS " + qos + "\n");
			resources.remove(qosResource);
		}
		
		// node constraints
		ResourceRequest nodeConstraints=ResourceRequest.find(resources, ResourceSet.NODE_CONSTRAINTS);
		if(nodeConstraints!=null){
			String nc=checkLegal(nodeConstraints.getRequestedValue(),"Node constraints");
			if(nc!=null){
				commands.append("#TSI_BSS_NODES_FILTER " + nc + "\n");
			}
			resources.remove(nodeConstraints);
		}

		// job array size and limit
		ResourceRequest arraySizeResource=ResourceRequest.find(resources, ResourceSet.ARRAY_SIZE);
		if(arraySizeResource!=null){
			ResourceRequest arrayLimitResource = null;
			int size = Integer.parseInt(arraySizeResource.getRequestedValue())-1;
			if(size>0){
				commands.append("#TSI_ARRAY 0-" + size + "\n");
				arrayLimitResource=ResourceRequest.find(resources, ResourceSet.ARRAY_LIMIT);
				if(arrayLimitResource!=null){
					commands.append("#TSI_ARRAY_LIMIT " + arrayLimitResource.getRequestedValue() + "\n");
				}
			}
			resources.remove(arraySizeResource);
			if(arrayLimitResource!=null)resources.remove(arrayLimitResource);
		}

		// CPUs / nodes
		ResourceRequest totalCpusRequest=ResourceRequest.find(resources, ResourceSet.TOTAL_CPUS);
		if (totalCpusRequest!= null) {
			try {
				resources.remove(totalCpusRequest);
				total_processors = Integer.valueOf(totalCpusRequest.getRequestedValue());
			} catch (RuntimeException e) {
				// ignore
			}
		}
		ResourceRequest cpusRequest=ResourceRequest.find(resources, ResourceSet.CPUS_PER_NODE);
		if(cpusRequest!=null){
			try {
				resources.remove(cpusRequest);
				processors_per_node = Integer.valueOf(cpusRequest.getRequestedValue());
			} catch (RuntimeException e) {
				// ignore
			}
		}
		ResourceRequest nodesRequest=ResourceRequest.find(resources, ResourceSet.NODES);
		if (nodesRequest!= null) {
			try {
				resources.remove(nodesRequest);
				nodes = Integer.valueOf(nodesRequest.getRequestedValue());
			} catch (RuntimeException e) {
				// ignore
			}
		}

		// memory per node
		ResourceRequest memoryRequest=ResourceRequest.find(resources, ResourceSet.MEMORY_PER_NODE);
		if (memoryRequest!= null) {
			try {
				resources.remove(memoryRequest);
				memory = Integer.valueOf(memoryRequest.getRequestedValue()) / MEGABYTE;
			} catch (RuntimeException e) {
				// ignore
			}
		}

		// time, individual == wall clock time
		run_time = getRuntime(resources);
		ResourceRequest.removeQuietly(resources, ResourceSet.RUN_TIME);

		//total number of processors
		//int total=nodes!=-1? nodes*processors: processors;

		if(run_time>0){
			commands.append("#TSI_TIME " + (int) run_time + "\n");
		}

		if(memory>0){
			commands.append("#TSI_MEMORY " + (int) memory + "\n");
		}

		// can also have nodes not set at all (TSI does the mapping based on
		// total number of processors)

		commands.append("#TSI_NODES " + nodes + "\n");
		commands.append("#TSI_PROCESSORS_PER_NODE " + processors_per_node + "\n");
		commands.append("#TSI_TOTAL_PROCESSORS " + total_processors + "\n");

		commands.append("#TSI_QUEUE " + queue + "\n");

		// Set some resources as environment variables
		commands.append("UC_NODES=" + nodes + "; export UC_NODES;\n");
		commands.append("UC_PROCESSORS_PER_NODE=" + processors_per_node
				+ "; export UC_PROCESSORS_PER_NODE;\n");
		commands.append("UC_TOTAL_PROCESSORS=" + total_processors
				+ "; export UC_TOTAL_PROCESSORS;\n");


		commands.append("UC_RUNTIME=" + (int)run_time
				+ "; export UC_RUNTIME;\n");

		commands.append("UC_MEMORY_PER_NODE=" + (int)memory
				+ "; export UC_MEMORY_PER_NODE;\n");

		// add dynamic/site-specific ones
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
	public static void appendEnvironment(StringBuilder commands,
			ExecutionContext ec, boolean filter) {
		if (ec.getUmask() != null) {
			commands.append("#TSI_UMASK " + ec.getUmask() + "\n");
			commands.append("umask " + ec.getUmask() + "\n");	
		}
		for (Map.Entry<String, String> env : ec.getEnvironment().entrySet()) {
			String key = env.getKey();
			if(filter&&filteredVariables.contains(key)){
				continue;
			}
			String value = env.getValue();
			commands.append(key).append("=\"").append(value).append(
					"\"; export " + key + "\n");
		}
	}

	protected static void appendSiteSpecificResources(StringBuilder commands,
			List<ResourceRequest> rs) {
		try {
			for (ResourceRequest r : rs) {
				try {
					String name = r.getName().replace(' ', '_');
					String value = checkLegal(r.getRequestedValue(),name);
					commands.append("#TSI_SSR_" + name.toUpperCase() + " "
							+ value + "\n");
				} catch (Exception e) {
					// ignored
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * compares versions
	 * @param tsiVersion - the current TSI version
	 * @param minRequired - the mininum required version
	 * @return <code>true</code> if tsiVersion >= minRequired, <code>false</code> otherwise
	 */
	public static boolean compareVersion(String tsiVersion, String minRequired) {
		if(tsiVersion==null)return false;

		String[] curS = tsiVersion.split("\\.");
		String[] reqS = minRequired.split("\\.");
		int[] cur = new int[curS.length];
		int[] req = new int[reqS.length];
		try{
			for (int i=0; i<curS.length; i++)
				cur[i] = Integer.parseInt(curS[i]);
			for (int i=0; i<reqS.length; i++)
				req[i] = Integer.parseInt(reqS[i]);
		}
		catch(NumberFormatException ex){
			return false;
		}
		for (int i=0; i<Math.min(cur.length, req.length); i++) {
			if (cur[i] < req[i])
				return false;
			else if (cur[i] > req[i])
				return true;
		}
		if (cur.length >= req.length)
			return true;
		return false;
	}

	/**
	 * Prepares a standard groups string for a TSI call requesting membership in all groups configured
	 * by attribute sources and possibly refined by user preferences.
	 * @param client
	 * @param tsiVersion
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
	 * @param tsiVersion
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

	public static String makePauseCommand(String bssid) {
		final StringBuffer commands = new StringBuffer();
		commands.append("#TSI_HOLDJOB\n");
		commands.append("#TSI_BSSID " + bssid + "\n");
		return commands.toString();
	}

	public static String makeResumeCommand(String bssid) {
		final StringBuffer commands = new StringBuffer();
		commands.append("#TSI_RESUMEJOB\n");
		commands.append("#TSI_BSSID " + bssid + "\n");
		return commands.toString();
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
			} catch (RuntimeException e) { /* ignore */ }
		}

		return runtime;
	}


	private static String[] illegal = {"\n","\r","`","$("};

	/**
	 * Check if the input string contains illegal characters. If the string contains
	 * newline characters, backticks or "$(..)", an IllegalArgumentException is thrown.
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
	 * up the script sent to the Perl TSI
	 * 
	 * @param input - the input string coming from an untrusted source
	 * @return sanitized string with single quotes replaced by a properly quoted version
	 */
	public static String sanitize(String input){
		String sane = input.replace("'","'\\''");
		return sane;
	}
}
