/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.ParseException;

import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;


/**
 * This action evaluates SpEL expression, result treat as command line. The
 * command is invoked with original script as stdin, stdout of the command is returned.
 * 
 * @author golbi
 *
 */
public class SpelFilter extends ScriptActionBase implements AfterAction
{
	public static final String ID = "script";
	private Expression expression;
	
	public SpelFilter(String expr, int maxWait) throws ParseException
	{
		super(maxWait);
		this.expression = TweakerConfiguration.parseSpEL(new SpelExpressionParser(),
				expr, new TemplateParserContext("${", "}"));
	}

	@Override
	public String filter(RootCtxBean root, String script) throws Exception
	{
		Process p = runScript(expression, root);
		
		TimeLimitedThread tlthread = new TimeLimitedThread(p, script);
		Thread thread = new Thread(tlthread);
		thread.start();
		
		waitFor(p, thread, tlthread);

		return new String(tlthread.buf, 0, tlthread.length, Charset.defaultCharset());
	}

	@Override
	public String getActionDefinition()
	{
		return expression.getExpressionString();
	}
	
	private static class TimeLimitedThread extends TimeLimitedThreadBase
	{
		private byte buf[];
		private int length;
		private String script;
		
		public TimeLimitedThread(Process p, String script)
		{
			super(p);
			this.script = script;
		}
		
		public void unsafeRun() throws Exception
		{
			OutputStream os = p.getOutputStream();
			byte[] orig = script.getBytes(Charset.defaultCharset());
			os.write(orig);
			os.flush();
			os.close();
			InputStream is = p.getInputStream();
			buf = new byte[Math.max(orig.length*10, 10240)];
			int i=0;
			int r = 0;
			while ((r=is.read(buf, i, buf.length-i)) > 0)
				i += r;
			if (r != -1)
				throw new Exception("Filtered script produced by the command is too long. " +
						"Maximal length is 10kb or 10 times legth of the original script whichever is bigger.");
			length = i;
			super.unsafeRun();
		}
	}
}
