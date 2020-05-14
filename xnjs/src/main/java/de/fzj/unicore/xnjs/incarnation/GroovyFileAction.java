/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.IOException;




/**
 * This action simply evaluates the Groovy script. The script is read form the provided file. 
 * @author golbi
 */
public class GroovyFileAction extends GroovyAction
{
	public static final String ID = "groovy-file";
	
	public GroovyFileAction(String groovyScript) throws IOException
	{
		super(GroovyFileFilter.readFromFile(groovyScript));
	}
}
