package de.fzj.unicore.uas.security;


import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.log4j.Logger;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SignatureStatus;
import eu.unicore.security.wsutil.AuthInHandler;
import eu.unicore.security.wsutil.DSigSecurityInHandler;
import eu.unicore.util.Log;


/**
 * For interop, it might be necessary to switch off signature checks for certain
 * services (e.g. BES-related services). This can be achieved by adding this 
 * security handler to the service in question. The handler basically claims
 * that the message was signed correctly, independently of the real outcome
 * of the signature check.
 * @see SecurityTokens
 * @author B. Demuth
 */
public class SkipSignatureCheckInHandler extends AbstractSoapInterceptor
{
	private static Logger logger = Log.getLogger(Log.SECURITY + ".dsig",
			SkipSignatureCheckInHandler.class);
	
	public SkipSignatureCheckInHandler()
	{
		super(Phase.PRE_INVOKE);
		getAfter().add(DSigSecurityInHandler.class.getName());
		getAfter().add(AuthInHandler.class.getName());
	}
	
	@Override
	public void handleMessage(SoapMessage ctx)
	{
		SecurityTokens securityTokens = (SecurityTokens) ctx.get(SecurityTokens.KEY);
		if (securityTokens == null)
		{
			logger.error("No security context found. You should add " + 
					AuthInHandler.class.getName() + " handler.");
			return;
		}
		
		securityTokens.setMessageSignatureStatus(SignatureStatus.OK);
		String soapAction=(String)securityTokens.getContext().get(SecurityTokens.CTX_SOAP_ACTION);
		if(logger.isDebugEnabled()) 
		{
			logger.debug("Signature verification skipped for action "+soapAction);
		}
	}
	
	

}



