package de.fzj.unicore.uas.security.testservices;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;


@WebService
public interface IExample {

	@WebMethod(action="testGetTimeAction")
	public CurrentTimeDocument getTime(GetResourcePropertyDocument in);
	
}
