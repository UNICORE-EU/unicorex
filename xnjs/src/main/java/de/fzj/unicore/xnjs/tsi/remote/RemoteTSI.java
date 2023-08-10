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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.ACLEntry;
import de.fzj.unicore.xnjs.io.ACLEntry.Type;
import de.fzj.unicore.xnjs.io.ACLSupportCache;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.io.ChangeACL.ACLChangeMode;
import de.fzj.unicore.xnjs.io.ChangePermissions;
import de.fzj.unicore.xnjs.io.FileFilter;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileImpl;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.io.XnjsStorageInfo;
import de.fzj.unicore.xnjs.tsi.BatchMode;
import de.fzj.unicore.xnjs.tsi.MultiNodeTSI;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import de.fzj.unicore.xnjs.util.BackedInputStream;
import de.fzj.unicore.xnjs.util.BackedOutputStream;
import de.fzj.unicore.xnjs.util.ErrorCode;
import de.fzj.unicore.xnjs.util.IOUtils;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * a TSI that talks to the UNICORE TSI daemon
 * 
 * @author schuller
 * @author golbi
 */
public class RemoteTSI implements MultiNodeTSI, BatchMode {

	private static final Logger tsiLogger=LogUtil.getLogger(LogUtil.TSI,RemoteTSI.class);

	private StringBuilder commands=new StringBuilder();

	@Inject
	private TSIProperties tsiProperties;

	@Inject
	private TSIConnectionFactory factory;

	@Inject
	private TSIMessages tsiMessages;

	@Inject
	private ACLSupportCache aclSupportCache;

	private String user = "nobody";
	private String group = "NONE";
	private ExecutionContext ec;
	private boolean autoCommit;

	private Client client;

	private final String fileSeparator = "/";

	private String storageRoot="/";

	private String preferredHost=null;

	private String lastUsedTSIHost=null;

	private int umask = DEFAULT_UMASK;

	// timeout waiting for a TSI connection (before creating a new one)
	static final int timeout = 5000;

	@Override
	public void setPreferredTSIHost(String host){
		this.preferredHost=host;
	}

	@Override
	public String getLastUsedTSIHost(){
		return lastUsedTSIHost;
	}

	@Override
	public void setClient(Client client){
		this.client=client;
		if(client!=null){
			user=client.getSelectedXloginName();
			group = TSIMessages.prepareGroupsString(client);
		} else {
			user="nobody";
			group="NONE";
		}
	}

	protected String extractCredentials(){
		try{
			return client.getSecurityTokens().getUserPreferences().get("UC_OAUTH_BEARER_TOKEN")[0];
		}catch(Exception ex){}
		return null;
	}

	@Override
	public boolean isLocal(){
		return false;
	}

	private boolean transactionInProgress=false;

	@Override
	public void startBatch() throws ExecutionException {
		doBegin();
		transactionInProgress=true;
	}

	@Override
	public String commitBatch() throws ExecutionException{
		transactionInProgress=false;
		return doCommit();
	}

	private void doBegin()throws ExecutionException{
		commands=new StringBuilder();
	}

	private void begin()throws ExecutionException{
		if(!transactionInProgress)doBegin();
	}

	private String commit()throws ExecutionException{
		return !transactionInProgress? doCommit() : null;
	}

	private String doCommit()throws ExecutionException{
		if(commands.length()==0)return null;
		String tsiCmd = tsiMessages.makeExecuteScript(commands.toString(), ec, extractCredentials());
		return doTSICommand(tsiCmd);
	}

	@Override
	public void cleanupBatch(){
		transactionInProgress=false;
		commands=new StringBuilder();
	}

	/**
	 * Make relative path absolute by prepending the storage root and cleanup lone quotes.
	 * Also, include the path in single quotes "'" so the shell will not process the 
	 * content -> avoid user command injection
	 */
	private String makeQuotedTarget(String target){
		return "'"+makeTarget(target,true)+"'";
	}

	/**
	 * make relative path absolute by prepending the storage root
	 * @param relativePath - the path
	 * @param sanitize - if <code>true</code> lone quotes will be escaped. 
	 *                   ALL strings that end up in TSI scripts must be sanitized
	 * @return sanitized absolute path
	 */
	private String makeTarget(String relativePath, boolean sanitize){
		String target = IOUtils.getNormalizedPath(getStorageRoot()+fileSeparator+relativePath);
		return sanitize ? TSIMessages.sanitize(target) : target;
	}

	/**
	 * make relative path absolute by prepending the storage root and cleanup lone quotes
	 */
	private String makeTarget(String target){
		return makeTarget(target,true);
	}

	private TSIConnection getConnection() throws TSIUnavailableException {
		lastUsedTSIHost = "n/a";
		TSIConnection c = factory.getTSIConnection(user, group, preferredHost, timeout);
		lastUsedTSIHost = c.getTSIHostName();
		return c;
	}

	private String doTSICommand(String tsiCmd)throws ExecutionException{
		return doTSICommandLowLevel(tsiCmd, group);
	}

	private String doTSICommandWithAllGroups(String tsiCmd) throws ExecutionException{
		String groups = null;
		if (client!=null){
			groups = TSIMessages.prepareAllGroupsString(client);
		}
		return doTSICommandLowLevel(tsiCmd, groups);
	}

	private String doTSICommandLowLevel(String tsiCmd, String groups) throws ExecutionException {
		String res="";
		try(TSIConnection c = getConnection()){
			res = c.send(tsiCmd);
			if(!res.contains("TSI_OK")){
				String msgShort="Command execution on TSI <"+lastUsedTSIHost+"> failed. Reply was: \n"+res;
				ErrorCode err = new ErrorCode(ErrorCode.ERR_TSI_EXECUTION, msgShort);
				throw new ExecutionException(err);
			}
		}
		catch(IOException ioe){
			String msgShort = Log.createFaultMessage("Command execution on TSI <"+lastUsedTSIHost+"> failed.",ioe);
			ErrorCode err = new ErrorCode(ErrorCode.ERR_TSI_COMMUNICATION, msgShort);
			throw new ExecutionException(err);
		}
		finally{
			begin();
		}
		return res;
	}

	@Override
	public void chmod(String file, Permissions perm) throws ExecutionException {
		begin();
		String target=makeQuotedTarget(file);
		commands.append(tsiProperties.getValue(TSIProperties.TSI_CHMOD)+
				" u="+perm.toChmodString()+" "+target+"\n");
		commit();
	}

	private String canonPerms(String original) {
		char[] chars = original.toCharArray();
		StringBuilder ret = new StringBuilder();
		for (char c: chars)
			if (c == 'r' || c == 'x' || c== 'w' || c == 'X')
				ret.append(c);

		return ret.toString();
	}

	@Override
	public void chmod2(String file, ChangePermissions[] perms, boolean recursive)
			throws ExecutionException {
		begin();
		String target=makeQuotedTarget(file);
		for (ChangePermissions perm: perms) {
			String pperms = canonPerms(perm.getPermissions());
			String rec = recursive ? "-R " : "";
			commands.append(tsiProperties.getValue(TSIProperties.TSI_CHMOD) 
					+ " " + rec + perm.getClazzSymbol() +  
					perm.getModeOperator() + pperms + " " + target + "\n");
		}
		String reply = commit();
		checkNoErrors(reply);
	}

	@Override
	public void chgrp(String file, String newGroup, boolean recursive)
			throws ExecutionException {
		begin();
		String target = makeQuotedTarget(file);
		String rec = recursive ? "-R " : "";
		commands.append(tsiProperties.getValue(TSIProperties.TSI_CHGRP) + " " + rec + newGroup + " " + target + "\n");
		String reply = commit();
		checkNoErrors(reply);
	}

	@Override
	public void cp(String source, String target) throws ExecutionException {
		begin();
		commands.append(tsiProperties.getValue(TSIProperties.TSI_UMASK) + " " + Integer.toOctalString(umask) + "\n");
		commands.append(tsiProperties.getValue(TSIProperties.TSI_CP)+" "+makeQuotedTarget(source)+" "+makeQuotedTarget(target)+"\n");
		String reply = commit();
		checkNoErrors(reply);
	}

	@Override
	public void link(String target, String linkName) throws ExecutionException {
		begin();
		commands.append(tsiProperties.getValue(TSIProperties.TSI_LN)+" "+makeQuotedTarget(target)+" "+makeQuotedTarget(linkName)+"\n");
		String reply = commit();
		checkNoErrors(reply);
	}

	@Override
	public void rename(String source, String target) throws ExecutionException {
		begin();
		commands.append(tsiProperties.getValue(TSIProperties.TSI_UMASK) + " " + Integer.toOctalString(umask) + "\n");
		commands.append(tsiProperties.getValue(TSIProperties.TSI_MV)+" "+makeQuotedTarget(source)+" "+makeQuotedTarget(target)+"\n");
		String reply = commit();
		checkNoErrors(reply);
	}

	@Override
	public void exec(String what, ExecutionContext ec) throws ExecutionException {
		begin();
		commands.append(tsiProperties.getValue(TSIProperties.TSI_CD)+" \""+ec.getWorkingDirectory()+"\"\n");
		commands.append(tsiProperties.getValue(TSIProperties.TSI_UMASK) + " " + Integer.toOctalString(umask) + "\n");
		commands.append(what);
		this.ec=ec;
		commit();
	}

	@Override
	public void execAndWait(String what, ExecutionContext ec) throws ExecutionException {
		if(transactionInProgress)throw new IllegalStateException("execAndWait() cannot be invoked in a batch");
		try{
			begin();
			commands.append(tsiProperties.getValue(TSIProperties.TSI_CD)+" \""+ec.getWorkingDirectory()+"\"\n");
			commands.append(tsiProperties.getValue(TSIProperties.TSI_UMASK) + " " + Integer.toOctalString(umask) + "\n");
			if(ec!=null && ec.getStdout()!=null){
				what = what+" > "+ec.getStdout();
			}
			if(ec!=null && ec.getStderr()!=null){
				what = what+" 2> "+ec.getStderr();
			}

			commands.append(what);
			this.ec=ec;
			ec.setRunOnLoginNode(true);
			commit();
			tsiLogger.debug("Executed "+what+" in "+ec.getWorkingDirectory());
			//get exit code
			String oldRoot=storageRoot;
			storageRoot="/";
			try(InputStream is = getInputStream(ec.getWorkingDirectory()+"/"+TSIMessages.EXITCODE_FILENAME);
				BufferedReader br =new BufferedReader(new InputStreamReader(is)))
			{
				String s=br.readLine();
				int i=Integer.parseInt(s);
				ec.setExitCode(i);
				tsiLogger.debug("Script exited with code <"+i+">");
			}finally{
				storageRoot=oldRoot;
			}
		}catch(Exception e){
			throw new ExecutionException("Can't execute script.",e);
		}
	}

	public boolean getAutoCommit() throws ExecutionException {
		return autoCommit;
	}

	@Override
	public void mkdir(String dir) throws ExecutionException {
		begin();
		String target = makeQuotedTarget(dir);
		commands.append(tsiProperties.getValue(TSIProperties.TSI_MKDIR) 
				+ " -m " + TSIMessages.getDirPerm(umask) 
				+ " "+ target + "\n");
		String reply = commit();
		checkNoErrors(reply);
	}

	@Override
	public String getFileSeparator(){
		return fileSeparator;
	}

	@Override
	public String getHomePath()throws ExecutionException{
		return getEnvironment("HOME");
	}

	@Override
	public String getEnvironment(String name)throws ExecutionException{
		return doExecuteScript("echo ${"+name+"}");
	}

	@Override
	public String resolve(String name)throws ExecutionException{
		return doExecuteScript("echo \""+name+"\"");
	}

	@Override
	public void rm(String target) throws ExecutionException {
		begin();
		commands.append(tsiProperties.getValue(TSIProperties.TSI_RM)+" "+makeQuotedTarget(target)+"\n");
		String reply = commit();
		checkNoErrors(reply);
	}

	@Override
	public void rmdir(String target) throws ExecutionException {
		begin();
		commands.append(tsiProperties.getValue(TSIProperties.TSI_RMDIR)+" "+makeQuotedTarget(target)+"\n");
		String reply = commit();
		checkNoErrors(reply);
	}

	/**
#
#   Listing starts with the line:
#
#   START_LISTING
#
#   and ends with the line:
#
#   END_LISTING
#
#   The files are listed in depth-first order. Each time a sub-directory 
#   is found the entry for the sub-directory file is listed and then entries 
#   for all the file in the subdirectory are listed.
#
#   The format for each listing line is:
#
#      Character 0 is usually blank, except:
#
#            If character 0 is '-', then the this line contains extra 
#            information about the file described in the previous line. 
#            This line is copied without change into the ListDirectory 
#            outcome entry for the file.
#
#            If character 0 is '<', then all files in a sub-directory 
#            have been listed and the listing is continuing with the parent 
#            directory. This is required even when the listing is non-recursive.
#
#     Character 1 is 'D' if the file is a directory
#
#     Character 2 is "R" if the file is readable by the Xlogin (effective uid/gid)
#
#     Character 3 is "W" if the file is writable by the Xlogin (effective uid/gid)
#
#     Character 4 is "X" if the file is executable by the Xlogin (effective uid/gid)
#
#     Character 5 is "O" if the file is owned by the Xlogin (effective uid/gid)
#
#     Character 6 is a space.
#
#     Until the next space is a decimal integer which is the size of the file in bytes.
#
#     Until the next space is a decimal integer which is the last modification 
#     time of the file in seconds since the Unix epoch.
#
#     Until the end of line is the full path name of the file
#
#     Every line is terminated by \n
#
	 * 
	 */
	@Override
	public XnjsFile[] ls(String base,int offset, int limit, boolean filter)throws ExecutionException{
		String res = doLS(base,true,false);
		List<XnjsFile>files=new ArrayList<>();
		BufferedReader br=new BufferedReader(new StringReader(res+"\n"));
		int pos=0;
		while(true){
			try {
				String[] lines = tsiMessages.readTSILSLine(br);
				if(lines[0]==null)break;
				pos++;
				if(pos<=offset)continue;
				XnjsFile xFile=parseLine(lines);
				if (!filter || xFile.isOwnedByCaller() 
						|| xFile.getPermissions().isAccessible()) {
					files.add(xFile);
				}
			}catch(IllegalArgumentException iae) {
				tsiLogger.warn(Log.createFaultMessage("Error parsing TSI_LS  reply: "+res, iae));
			}
			if(files.size()==limit)break;
		}
		return (XnjsFile[])files.toArray(new XnjsFile[0]);

	}

	@Override
	public XnjsFile[] ls(String base)throws ExecutionException{
		return ls(base,0,Integer.MAX_VALUE,false);
	}

	@Override
	public XnjsFile[] find(String path, FileFilter filter, int offset, int limit)throws ExecutionException{
		boolean recurse=filter!=null && filter.recurse();
		String res = doLS(path,true,recurse);
		List<XnjsFile>files = new ArrayList<>();
		BufferedReader br=new BufferedReader(new StringReader(res+"\n"));
		int pos=0;
		while(true){
			try {
				String[] lines = tsiMessages.readTSILSLine(br);
				if(lines[0]==null)break;
				pos++;
				if(pos<=offset)continue;
				XnjsFile f=parseLine(lines);
				if(filter!=null && !filter.accept(f, this)){
					continue;
				}
				files.add(f);
			}catch(IllegalArgumentException iae) {
				tsiLogger.warn(Log.createFaultMessage("Error parsing TSI_LS reply: "+res, iae));
			}
			if(files.size()==limit)break;
		}
		return (XnjsFile[])files.toArray(new XnjsFile[files.size()]);
	} 

	@Override
	public XnjsFileWithACL getProperties(String path)throws ExecutionException{
		String res = doLS(path, false, false);
		ArrayList<XnjsFileImpl>files=new ArrayList<XnjsFileImpl>();
		BufferedReader br=new BufferedReader(new StringReader(res+"\n"));
		while(true){
			try {
				String[] lines = tsiMessages.readTSILSLine(br);
				if(lines[0]!=null){
					files.add(parseLine(lines));
				}else break;
			}catch(IllegalArgumentException iae) {
				tsiLogger.warn(Log.createFaultMessage("Error parsing TSI_LS  reply: "+res, iae));
			}
		}
		if(files.size()<1)return null;

		XnjsFileImpl ret = files.get(0);
		getfacl(path, ret);
		return ret;
	}

	/**
	 * call tsi_ls and return the result
	 * 
	 * @param file path relative to storage root
	 * @param normal true if "normal" directory listing, false if single file
	 * @param recurse - true if listing should include subdirs
	 * @return tsi response
	 * @throws Exception
	 */
	public String doLS(String file,boolean normal, boolean recurse)throws ExecutionException{
		return runTSICommand(tsiMessages.makeLSCommand(makeTarget(file), normal, recurse));	
	}

	private String doExecuteScript(String cmd)throws ExecutionException {
		String tsicmd = tsiMessages.makeExecuteScript(cmd, null, extractCredentials());
		try(TSIConnection conn = getConnection()){
			try {
				String res = conn.send(tsicmd);
				if(!res.contains("TSI_OK")){
					String msg="Command execution on TSI <"+lastUsedTSIHost+"> failed. TSI reply:" + res;
					ErrorCode err = new ErrorCode(ErrorCode.ERR_TSI_EXECUTION, msg);
					throw new ExecutionException(err);
				}
				return res.replace("TSI_OK", "").trim();
			}catch(IOException ioe) {
				throw new ExecutionException(Log.createFaultMessage("Command execution on TSI <"
						+lastUsedTSIHost+"> failed.", ioe));
			}
		}
	}

	/**
	 * for the format see the comment for ls()
	 * @param lines - one or two TSI listing lines
	 * @return a XnjsFile describing the file
	 */
	protected XnjsFileImpl parseLine(String[] lines){
		String[] tok=lines[0].substring(7).split(" ",3);
		int l=tok.length;
		String fullPath=tok[l-1];
		boolean isDirectory=lines[0].charAt(1)=='D';
		boolean isReadable=lines[0].charAt(2)=='R';
		boolean isWritable=lines[0].charAt(3)=='W';
		boolean isExecutable=lines[0].charAt(4)=='X';
		boolean isOwnedByCaller=lines[0].charAt(5)=='O';
		long size=Long.parseLong(tok[l-3]);

		//convert time in seconds from TSI into millis
		long lastMod=Long.parseLong(tok[l-2])*1000;

		Permissions p=new Permissions(isReadable,isWritable,isExecutable);
		String name=IOUtils.getRelativePath(fullPath, storageRoot);

		String unixPerms = null;
		String owner = null;
		String owningGroup = null;

		if (lines[1] != null) {
			unixPerms = lines[1].substring(2, 11);
			String[] ownership = lines[1].substring(12).split(" ", 3);
			owner = ownership[0];
			owningGroup = ownership[1];
		}

		XnjsFileImpl f=new XnjsFileImpl(name,size,isDirectory,lastMod,p,
				isOwnedByCaller, owner, owningGroup, unixPerms);
		return f;

	}

	/**
	 * call tsi_df and return the result
	 * 
	 * @param path - the path denoting the partition to be checked 
	 * 
	 * @throws ExecutionException
	 */
	public XnjsStorageInfo doDF(String path)throws ExecutionException{
		String cmd = tsiMessages.makeDFCommand(path);
		String res = null;
		try{
			res = runTSICommand(cmd);	
			return parseDFReply(res);
		}catch(Exception ex){
			throw new ExecutionException("Error executing TSI_DF. Reply was "+res, ex);
		}
	}

	private XnjsStorageInfo parseDFReply(String dfReply)throws ExecutionException{
		XnjsStorageInfo info=new XnjsStorageInfo();
		BufferedReader br=new BufferedReader(new StringReader(dfReply+"\n"));
		while(true){
			String line = tsiMessages.readTSIDFLine(br);
			if(line==null)break;
			String[]parts=line.split(" ");
			String key=parts[0];
			long value=Long.valueOf(parts[1]);
			if("TOTAL".equalsIgnoreCase(key)){
				info.setTotalSpace(value);
			}
			else if("FREE".equalsIgnoreCase(key)){
				info.setFreeSpace(value);
			}
			else if("USER".equalsIgnoreCase(key)){
				info.setUsableSpace(value);
			}
		}
		return info;
	}

	/**
	 * get the current user's remaining compute time budget or -1 if not known
	 * 
	 * @throws ExecutionException
	 */
	public List<BudgetInfo> getComputeTimeBudget() throws ExecutionException {
		String cmd = tsiMessages.makeGetBudgetCommand();
		String res = null;
		try{
			res = runTSICommand(cmd);	
			return parseGetComputeBudgetReply(res);
		}catch(Exception ex){
			String msg = Log.createFaultMessage("Error executing TSI_GET_COMPUTE_BUDGET", ex)
					+" TSI reply was "+res;
			throw new ExecutionException(msg);
		}
	}

	private List<BudgetInfo> parseGetComputeBudgetReply(String reply)throws ExecutionException {
		List<BudgetInfo> budget = new ArrayList<>();
		BufferedReader br=new BufferedReader(new StringReader(reply+"\n"));

		while(true){
			String line = tsiMessages.readTSIDFLine(br);
			if(line==null)break;
			try {
				budget.add(new BudgetInfo(line));
			}catch(Exception ex) {
				Log.logException("Could not parse compute budget reply item <"+line+">", ex, tsiLogger);
			}
		}
		return budget;
	}

	public List<String> getUserPublicKeys() throws ExecutionException {
		String cmd = tsiMessages.makeGetUserInfoCommand();
		String res = null;
		try{
			res = runTSICommand(cmd);	
			return parseGetUserInfoReply(res);
		}catch(Exception ex){
			String msg = Log.createFaultMessage("Error executing TSI_GET_USER_INFO", ex)
					+" TSI reply was "+res;
			throw new ExecutionException(msg);
		}
	}
	
	private List<String> parseGetUserInfoReply(String reply)throws ExecutionException {
		List<String> result = new ArrayList<>();
		BufferedReader br = new BufferedReader(new StringReader(reply+"\n"));
		while(true){
			String line = tsiMessages.readTSIDFLine(br);
			if(line==null)break;
			try {
				if(line.startsWith("Accepted key")) {
					result.add(line.split(":")[1]);
				}
			}catch(Exception ex) {
				Log.logException("Could not get_user_info reply item <"+line+">", ex, tsiLogger);
			}
		}
		return result;
	}

	/**
	 * read file from TSI<br/>
	 * 
	 * @param file - file path relative to storage root
	 */
	public InputStream getInputStream(final String file) throws ExecutionException {
		//figure out length of file
		final String target=makeTarget(file, false);

		return new BackedInputStream(tsiProperties.getIntValue(TSIProperties.TSI_BUFFERSIZE)) {

			@Override
			protected long getTotalDataSize() throws IOException {
				try{
					return readLength(file);
				}catch(ExecutionException ex){
					throw new IOException(ex);
				}
			}

			@Override
			protected void fillBuffer() throws IOException {
				//compute bytes to be read: either buffer size, or remainder of file
				long numBytes=Math.min(length-bytesRead,buffer.length);
				avail=readChunk(target,buffer,bytesRead,numBytes);
				pos=0;
				tsiLogger.debug("Read <{}> bytes into buffer.", avail);
			}
		};
	}


	/**
	 * read length of file by doing an ls()
	 * @param file - file path relative to storage root 
	 */
	protected long readLength(String file)throws ExecutionException{
		String res=doLS(file,false,false);
		long lengthFromLS=0;
		BufferedReader br=new BufferedReader(new StringReader(res+"\n"));
		while(true){
			try {
				String[] lines = tsiMessages.readTSILSLine(br);
				if(lines[0]!=null){
					lengthFromLS=parseLine(lines).getSize();
					break;
				}else break;
			}catch(IllegalArgumentException iae) {
				tsiLogger.warn(Log.createFaultMessage("Error parsing TSI_LS  reply: "+res, iae));
			}
		}
		return lengthFromLS;
	}

	/**
	 * read a chunk from the TSI
	 * @param file - absolute path 
	 */
	private int readChunk(String file,byte[] buf,long offset, long length)throws IOException{
		tsiLogger.debug("read from <{}> numbytes={}", file, length);
		String tsicmd = tsiMessages.makeGetFileChunkCommand(file,offset,length);
		try(TSIConnection conn = getConnection()){
			String res=conn.send(tsicmd);
			if(!res.contains("TSI_OK")){
				String msg="Command execution failed. TSI reply:"+res;
				ErrorCode ec=new ErrorCode(ErrorCode.ERR_TSI_EXECUTION,msg);
				throw new ExecutionException(ec);
			}
			else {
				tsiLogger.debug("TSI response: '{}'", res);
			}
			int av=0;
			BufferedReader br=new BufferedReader(new StringReader(res));
			while(true){
				String l=br.readLine();
				if(l==null) break;
				if(l.startsWith("TSI_LENGTH")){
					av= Integer.parseInt(l.split(" ")[1]);
					break;
				}
			}
			conn.getData(buf,0,av);
			conn.getLine(); //U4 NJS/TSI protocol: extra 'ENDOFMESSAGE' line
			return av;
		}catch(Exception e){
			IOException io = new IOException("Error reading from TSI");
			io.initCause(e);
			throw io;
		}
	}

	@Override
	public OutputStream getOutputStream(final String file,final boolean append,long numbytes) throws ExecutionException {
		final String target=makeTarget(file,false);
		return new BackedOutputStream (append, tsiProperties.getIntValue(TSIProperties.TSI_BUFFERSIZE)){

			@Override
			public void writeBuffer() throws IOException {
				final boolean doAppend=firstWrite? append: true;
				try {
					writeChunk(target, buffer, pos, doAppend);
				} catch (TSIUnavailableException e) {
					IOException ioe=new IOException("TSI unavailable.");
					ioe.initCause(e);
					throw ioe;
				}
				catch (ExecutionException e) {
					IOException ioe=new IOException("TSI error");
					ioe.initCause(e);
					throw ioe;
				}
			}
		};
	}

	@Override
	public OutputStream getOutputStream(final String file,final boolean append) throws ExecutionException {
		return getOutputStream(file, append, -1);
	}

	@Override
	public OutputStream getOutputStream(String file) throws ExecutionException {
		return getOutputStream(file,false,-1);
	}

	/**
	 * write a blob of data to the named file <br/>
	 * see XNJS 4 NJS BatchTargetSystem and TSI PutFiles.pm
	 * @param file - absolute file path
	 */
	private void writeChunk(String file, byte[] buf, int numBytes, boolean append)throws IOException,TSIUnavailableException, ExecutionException{
		tsiLogger.debug("Write to {}, append={}, numBytes={}", file, append, numBytes);
		try(TSIConnection conn = getConnection()){
			String permissions = TSIMessages.getFilePerm(umask) ;

			String tsicmd = tsiMessages.makePutFileChunkCommand(file, permissions, numBytes, append);
			String res=conn.send(tsicmd);
			if(!res.contains("TSI_OK")){
				throw new IOException("Execution on TSI <"+lastUsedTSIHost+"> failed. Reply was "+res);
			}
			conn.sendData(buf,0,numBytes);
			conn.getLine(); // extra ENDOFMESSAGE
		}
	}

	@Override
	public String getFileSystemIdentifier(){
		try{
			String fsID=tsiProperties.getValue(TSIProperties.TSI_FILESYSTEM_ID);
			if(fsID!=null){
				return fsID;
			}
			else{
				return "UNICORE TSI at "+factory.getTSIMachine();
			}
		}catch(Exception ex){
			return null;
		}
	}

	@Override
	public XnjsStorageInfo getAvailableDiskSpace(String path){
		try{
			return doDF(path);
		}catch(ExecutionException ex){
			LogUtil.logException("Could not determine disk space information", ex, tsiLogger);
			return XnjsStorageInfo.unknown();
		}
	}

	@Override
	public void setStorageRoot(String root) {
		this.storageRoot=root;
	}

	public String getStorageRoot() {
		return storageRoot;
	}

	@Override
	public String[] getGroups() throws ExecutionException {
		String cmd = tsiMessages.makeExecuteScript(tsiProperties.getValue(TSIProperties.TSI_GROUPS), ec, extractCredentials());
		String reply = doTSICommandWithAllGroups(cmd);
		try{
			String groups = tsiMessages.readTSIDFLine(new BufferedReader(new StringReader(reply)));
			return groups.split("\\s+");
		}
		catch(Exception ex){
			throw new ExecutionException(ex);
		}
	}

	private String faclCommon(String file, String command)  throws ExecutionException {
		String target = makeTarget(file);
		String tsicmd = command + 
				"#TSI_ACL_PATH " + target + "\n";
		tsiLogger.debug("TSI command: \n{}", tsicmd);
		String res;
		try(TSIConnection conn = getConnection()){
			try {
				res = conn.send(tsicmd);
			} catch (IOException e)	{
				throw new ExecutionException(
						"Problem sending ACL operation to TSI server.", e);
			}
		}
		if(!res.contains("TSI_OK")) {
			throw new ExecutionException(
					"ACL operation on TSI <"+lastUsedTSIHost+"> failed. Reply was " + res);
		}
		return res;
	}

	public void getfacl(String file, XnjsFileImpl ret) throws ExecutionException {
		if (!isACLSupported("/")) 
			return;

		String cmd = "#TSI_FILE_ACL\n" +
				"#TSI_ACL_OPERATION GETFACL\n";

		String res = faclCommon(file, cmd);

		String[] entries = res.split("\n");
		ACLEntry[] aclEntries;
		if (entries.length > 1)
			aclEntries = new ACLEntry[entries.length - 1];
		else
			aclEntries = new ACLEntry[0];
		for (int i=1; i<entries.length; i++) {
			String []subentries = entries[i].split(":");
			ACLEntry.Type type;
			boolean defaultACL;
			int off = 0;
			if (subentries[0].equals("default")) {
				defaultACL = true;
				off++;
			} else {
				defaultACL = false;
			}
			if (subentries[off].equals("group"))
				type = ACLEntry.Type.GROUP;
			else
				type = ACLEntry.Type.USER;

			aclEntries[i-1] = new ACLEntry(type, subentries[off+1], subentries[off+2], defaultACL);
		}
		ret.setACL(aclEntries);
	}

	@Override
	public void setfacl(String file, boolean clearAll, ChangeACL[] changeACL, boolean recursive)
			throws ExecutionException
	{
		if (!isACLSupported("/")) 
			throw new ExecutionException("Setting file ACLs is not supported on this storage.");

		String cmdBase = "#TSI_FILE_ACL\n" +
				"#TSI_ACL_OPERATION SETFACL\n";
		String recStr = recursive ? " RECURSIVE" : "";
		if (clearAll) {
			StringBuilder cmd = new StringBuilder();
			cmd.append(cmdBase);
			cmd.append("#TSI_ACL_COMMAND RM_ALL").append(recStr).append("\n");
			faclCommon(file, cmd.toString());
		}

		for (ChangeACL entry: changeACL) {
			StringBuilder cmd = new StringBuilder();
			cmd.append(cmdBase);
			cmd.append("#TSI_ACL_COMMAND ");
			if (entry.getChangeMode().equals(ACLChangeMode.REMOVE))
				cmd.append("RM").append(recStr).append("\n");
			else
				cmd.append("MODIFY").append(recStr).append("\n");

			cmd.append("#TSI_ACL_COMMAND_SPEC ");
			if (entry.isDefaultACL())
				cmd.append("D");
			if (entry.getType().equals(Type.GROUP))
				cmd.append("G ");
			else
				cmd.append("U ");
			cmd.append(entry.getSubject());
			cmd.append(" ");
			cmd.append(entry.getPermissions());
			cmd.append("\n");	

			faclCommon(file, cmd.toString());
		}
	}

	private boolean getACLSupportFromTSI(String path) throws ExecutionException {
		String cmd = "#TSI_FILE_ACL\n" +
				"#TSI_ACL_OPERATION CHECK_SUPPORT\n";
		String ret = faclCommon(path, cmd);
		return ret.contains("true");
	}

	@Override
	public boolean isACLSupported(String path) throws ExecutionException {
		Boolean cached = aclSupportCache.getACLCachedSupport(storageRoot, path);
		if (cached != null)
			return cached.booleanValue();
		boolean fromTSI = getACLSupportFromTSI(path);
		aclSupportCache.cacheACLSupport(storageRoot, path, fromTSI);
		return fromTSI;
	}

	@Override
	public void setUmask(String umask) {
		if (umask == null)
			this.umask = DEFAULT_UMASK;
		else
			this.umask = Integer.parseInt(umask, 8);
	}

	@Override
	public String getUmask() {
		return Integer.toOctalString(this.umask);
	}

	public void assertIsDirectory(String dir, String format, Object... args)throws ExecutionException{
		boolean noPreferredHost = false;
		if(preferredHost==null){
			preferredHost = lastUsedTSIHost;
			noPreferredHost = true;
		}
		XnjsFile f = getProperties(dir);
		if(f == null)throw new ExecutionException(String.format(format, args));
		if(!f.isDirectory())throw new ExecutionException(String.format(format+" File exists!", args));
		if(noPreferredHost)preferredHost = null;
	}

	public TSIConnectionFactory getFactory(){
		return factory;
	}

	public String runTSICommand(String command) throws ExecutionException {
		try(TSIConnection conn = getConnection()){
			String res=conn.send(command);
			if(res.startsWith("TSI_FAILED")){
				throw new ExecutionException("TSI ERROR: Error executing command on TSI <"+lastUsedTSIHost+">. TSI reply: "+res);
			}
			return res;
		}
		catch(IOException ioe){
			throw new ExecutionException(ioe);
		}
	}
	
	private void checkNoErrors(String reply) throws ExecutionException {
		if (reply==null)return;
		reply = reply.replaceFirst("TSI_OK", "").trim();
		if(reply.length()>0) {
			throw new ExecutionException(ErrorCode.ERR_TSI_EXECUTION, "TSI <"+lastUsedTSIHost+"> ERROR: '"+reply+"'");
		}
	}
	
	@Override
	public SocketChannel openConnection(String host, int port) throws Exception {
		return factory.connectToService(host, port, preferredHost, user, group);
	}
}
