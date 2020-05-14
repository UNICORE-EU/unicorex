/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 18-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

public class JavaErrorReporter
{
	/**
	 * To be used as standalone program simulating script returning error which should stop the job processiing.
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.err.print("Test ERROR");
		System.err.flush();
		System.exit(10);
	}

}
