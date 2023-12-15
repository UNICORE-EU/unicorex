package de.fzj.unicore.xnjs.io.impl;

import static de.fzj.unicore.xnjs.util.IOUtils.quote;

import java.net.URI;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;

/**
 * scp upload as defined by HPC file staging profile (GFD.135)
 * 
 * @author schuller
 */
public class ScpUpload extends AsyncFilemover{

	private final UsernamePassword credentials;
	
	private final IOProperties ioProperties;
	
	/**
	 * create an SCP upload
	 * 
	 * @param client
	 * @param workingDirectory
	 * @param source
	 * @param target
	 * @param config
	 * @param credentials - only UsernamePassword is supported
	 */
	public ScpUpload(Client client, String workingDirectory, String source, URI target, XNJS config, DataStagingCredentials credentials){
		super(client,workingDirectory,source,target.toString(),config);
		info.setProtocol("scp");
		if(credentials!=null && !(credentials instanceof UsernamePassword)){
			throw new IllegalArgumentException("Unsupported credential type <"+credentials.getClass().getName()
					+">, only UsernamePassword is supported.");
		}
		this.credentials=(UsernamePassword)credentials;
		ioProperties = config.getIOProperties();
	}

	public final boolean isImport(){
		return false;
	}
	
	public String makeCommandline()throws Exception{
		String scpWrapper=ioProperties.getValue(IOProperties.SCP_WRAPPER);
		String uri=info.getTarget();
		if(credentials!=null){
			//add username, but not password as per GFD.135
			uri=IOUtils.makeSCPAddress(uri, credentials.getUser());	
		}
		else throw new IllegalArgumentException("Username required for scp");
		
		StringBuilder sb=new StringBuilder();
		String full = quote(workingDirectory+"/"+info.getSource());
		sb.append(scpWrapper).append(" ").append(full).append(" ");
		sb.append(quote(uri));
		if(credentials!=null && credentials.getPassword()!=null){
			sb.append(" ").append(quote(credentials.getPassword()));
		}
		return sb.toString();
	}

}
