package de.fzj.unicore.uas.security.testservices;

import java.util.Calendar;

import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;

import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.ws.cxf.Servlet;

/**
 * for testing
 */
public class ExampleService implements IExample {

	public CurrentTimeDocument getTime(GetResourcePropertyDocument in) {
		//access security info
		initSecurityInfo();
		
		lastCallLocal=Servlet.getCurrentRequest()!=null;
		
		CurrentTimeDocument d=CurrentTimeDocument.Factory.newInstance();
		d.addNewCurrentTime().setCalendarValue(Calendar.getInstance());
		return d;
	}
	
	private void initSecurityInfo(){
		Client c=AuthZAttributeStore.getClient();
		SecurityTokens tokens=AuthZAttributeStore.getTokens();
		System.out.println(tokens);
		lastTokens=tokens;
		lastClient=c;
	}
	
	private static boolean lastCallLocal;
	public static boolean wasLastCallLocal(){
		return lastCallLocal;
	}

	private static SecurityTokens lastTokens;
	public static SecurityTokens getLastCallSecurityTokens(){
		return lastTokens;
	}
	
	private static Client lastClient;
	public static Client getLastCallClient(){
		return lastClient;
	}
	

}
