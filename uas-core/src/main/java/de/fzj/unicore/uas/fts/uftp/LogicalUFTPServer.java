package de.fzj.unicore.uas.fts.uftp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ExternalSystemConnector.Status;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Holds properties and parameters for set of UFTPD servers
 * that make up a single logical server that is used in 
 * round-robin mode
 *
 * @author schuller
 */
public class LogicalUFTPServer implements ISubSystem {

	public static final Logger log = Log.getLogger(Log.SERVICES, LogicalUFTPServer.class);
    
	private String statusMessage = "N/A";
	
	private String description = "n/a";

	private Status status = Status.NOT_APPLICABLE;
		
	private final Kernel kernel;

	private final List<UFTPDInstance> instances = new ArrayList<>();
	
	public LogicalUFTPServer(Kernel kernel){
		this.kernel = kernel;
		configure();
	}
	
	private void configure() {
		Properties properties = kernel.getContainerProperties().getRawProperties();
		String prefix = "coreServices.uftp.server.";
		String desc = properties.getProperty(prefix+"description", "n/a");
		setDescription(desc);
		if(properties.getProperty(prefix+"host")!=null) {
			UFTPDInstance server = createUFTPD("coreServices.uftp.", properties);
			instances.add(server);
		}
		else {
			int num = 1;
			while(true) {
				prefix = "coreServices.uftp."+num+".";
				if(properties.getProperty(prefix+"server.host")==null) {
					break;
				}
				UFTPDInstance server = createUFTPD(prefix, properties);
				instances.add(server);
				num++;
			}
		}
		log.info("Configured <"+instances.size()+"> UFTPD server(s)");
	}
	
	private UFTPDInstance createUFTPD(String prefix, Properties properties) {
		UFTPDInstance server = new UFTPDInstance(kernel);
		UFTPProperties props = new UFTPProperties(prefix, properties);
		server.configure(props);
		if(server.getHost()==null)throw new ConfigurationException("Property 'host' not set!");
		log.info("Configured "+server);
		return server;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String getStatusDescription(){
		checkConnection();
		return statusMessage;
	}

	public String toString(){
		return "[UFTPD Server "+getStatusDescription()+"]";
	}

	public String getName(){
		return "UFTPD Server";
	}

	public int getConfiguredServers() {
		return instances.size();
	}

	public boolean isUFTPAvailable(){
		if(instances.size()==0)return false;
		checkConnection();
		return Status.OK.equals(status);
	}
	
	private synchronized void checkConnection(){
		status = Status.DOWN;
		int avail = 0;
		for(UFTPDInstance i: instances){
			if(i.isUFTPAvailable()) {
				status = Status.OK;
				avail++;
				log.debug("UFTPD server {}:{} is UP: {}",i.getCommandHost(),i.getCommandPort(),i.getConnectionStatusMessage());
			}
			else {
				log.debug("UFTPD server {}:{} is DOWN: {}",i.getCommandHost(),i.getCommandPort(),i.getConnectionStatusMessage());
			}
		}
		if(instances.size()>1) {
			statusMessage = "OK ["+avail+" of "+instances.size()+" UFTPD servers available]";
		}else if(instances.size()==1){
			statusMessage = instances.get(0).getConnectionStatusMessage();	
		}else {
			statusMessage = "No UFTPD server configured.";
		}
	}

	int index = 0;
	
	public synchronized UFTPDInstance getUFTPDInstance() throws IOException {
		int c=0;
		while(c<=instances.size()) {
			UFTPDInstance i = instances.get(index);
			index++;
			if(index==instances.size())index=0;
			if(i.isUFTPAvailable()) {
				log.debug("Using {}: {}",index,i);
				return i;
			}
			c++;
		}
		throw new IOException("None of the configured UFTPD servers is available!");
	}
	
	@Override
	public Collection<ExternalSystemConnector>getExternalConnections(){
		Collection<ExternalSystemConnector>l = new ArrayList<>();
		l.addAll(instances);
		return l;
	}

}
