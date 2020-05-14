/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;




/**
 * This action reads a groovy script from a file, then evaluates it. 
 * Original TSI script is feed to 
 * the Groovy stdin, Groovy stdout is treat as the filtered TSI script.
 * 
 * @author golbi
 */
public class GroovyFileFilter extends GroovyFilter
{
	public static final String ID = "groovy-file";
	
	public GroovyFileFilter(String groovyScriptFile) throws IOException
	{
		super(readFromFile(groovyScriptFile));
	}

	public static String readFromFile(String groovyScriptFile) throws IOException
	{
		BufferedReader r = new BufferedReader(new FileReader(groovyScriptFile));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line=r.readLine()) != null)
		{
			sb.append(line);
			sb.append("\n");
		}
		r.close();
		return sb.toString();
	}
}
