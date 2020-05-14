/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 18-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.util.ErrorCode;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * Extended by classes which invoke operating system scripts
 * @author golbi
 *
 */
public abstract class ScriptActionBase
{
	protected static final Logger log = LogUtil.getLogger(LogUtil.XNJS, ScriptActionBase.class); 
	protected int maxWaitIter;
	protected final static int WAIT_T = 50;
	protected static final int RECOGNIZED_ERROR_CODE = 10;

	public ScriptActionBase(int maxWait)
	{
		maxWaitIter = maxWait/WAIT_T;
	}
	
	protected Process runScript(Expression expression, RootCtxBean root) throws Exception
	{
		EvaluationContext ctx = new StandardEvaluationContext(root);
		Object o = expression.getValue(ctx);
		if (o == null)
			throw new Exception("Script SpEL expression was evaluated to null");
		String cmdLine = o.toString();
		String[] cmdLineTokens = SimplifiedCmdLineLexer.tokenizeString(cmdLine);
		if (log.isDebugEnabled())
		{
			log.debug("Will run the following command " +
					"line (comma is used to separate arguments):\n" + 
					Arrays.toString(cmdLineTokens));
		}
		
		Runtime runtime = Runtime.getRuntime();
		return runtime.exec(cmdLineTokens);
	}
	
	protected void waitFor(Process p, Thread thread, TimeLimitedThreadBase tlthread) 
			throws Exception
	{
		try
		{
			thread.join(maxWaitIter*WAIT_T);
			if (thread.isAlive()) {
				throw new InterruptedException();
			}
		} catch (InterruptedException e)
		{
			p.destroy();
			thread.interrupt();
			throw new Exception("Killed the invoked script as it hang for at least " +
					maxWaitIter*WAIT_T + "ms");
		}
		
		if (tlthread.exc != null)
			throw tlthread.exc;
		int exitCode = 0;
		try
		{
			exitCode = p.exitValue();
		} catch (IllegalThreadStateException e)
		{
			//OK
		}
		if (exitCode == RECOGNIZED_ERROR_CODE)
			throw new ExecutionException(ErrorCode.ERR_EXECUTABLE_FORBIDDEN, tlthread.stderr);
		if (exitCode != 0)
			log.warn("Filter script finished with a non zero return code");
	}
	
	public static String readStdErr(Process p)
	{
		InputStream is = p.getErrorStream();
		byte[] buf = new byte[1024];
		int i=0;
		int r=0;
		String msg;
		try
		{
			while ((r=is.read(buf, i, buf.length-i)) > 0)
				i += r;
			msg = new String(buf, 0, i, Charset.defaultCharset());
		} catch (IOException e)
		{
			msg = "Error reading the command's stderr.";
		}
		return msg;
	}
	
	protected boolean finished(Process p)
	{
		try
		{
			p.exitValue();
		} catch (IllegalThreadStateException ie)
		{
			return false;
		}
		return true;
	}
	
	protected static class TimeLimitedThreadBase implements Runnable
	{
		protected Process p;
		protected String stderr;
		protected Exception exc = null;
		
		public TimeLimitedThreadBase(Process p)
		{
			this.p = p;
		}
		
		public void unsafeRun() throws Exception
		{
			stderr = readStdErr(p);
			p.waitFor();
		}

		@Override
		public void run()
		{
			try
			{
				unsafeRun();
			} catch (Exception e)
			{
				exc = e;
			}
		}
	}

}
