/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 18-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaCat
{
	/**
	 * To be used as standalone cat program (as we are not sure if /bin/cat is available ;-)
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ((line=is.readLine()) != null)
			System.out.println(line);
	}

}
