/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 18-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

public class JavaEcho
{
	/**
	 * To be used as standalone echo program (as we are not sure if /bin/echo is available ;-)
	 * @param args
	 */
	public static void main(String[] args)
	{
		for (String a: args)
			System.out.println('\'' + a + '\'');
	}

}
