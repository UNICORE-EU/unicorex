package eu.unicore.uas.fts.uftp;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Kernel;
import eu.unicore.util.Log;

/**
 * Inits UFTP configuration and connection to UFTPD
 * 
 * @author K. Benedyczak
 */
public class UFTPStartupTask implements Runnable {

	private static Logger logger = Log.getLogger(Log.CONFIGURATION, UFTPStartupTask.class);
	
	private Kernel kernel;
	
	public UFTPStartupTask(Kernel kernel) {
		this.kernel = kernel;
	}
	public void run() {
		try {
			setupUFTPConnector();
		}catch(Throwable ex) {
			Log.logException("UFTP connector could not be initialised, UFTP will not be available.", ex, logger);
		}
	}
	
	protected void setupUFTPConnector() {
		Properties cfg = kernel.getContainerProperties().getRawProperties();
		if(!Boolean.parseBoolean(cfg.getProperty(UFTPProperties.PREFIX+UFTPProperties.PARAM_ENABLE_UFTP, "true")))
		{
			logger.info("UFTP is disabled.");
			return;
		}
		LogicalUFTPServer connector = new LogicalUFTPServer(kernel);
		kernel.setAttribute(LogicalUFTPServer.class, connector);
		kernel.register(connector);
	}
}
