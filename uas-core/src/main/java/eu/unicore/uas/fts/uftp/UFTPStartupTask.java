package eu.unicore.uas.fts.uftp;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Kernel;
import eu.unicore.services.StartupTask;
import eu.unicore.util.Log;

/**
 * Inits UFTP configuration and connection to UFTPD
 * 
 * @author K. Benedyczak
 */
public class UFTPStartupTask implements StartupTask {

	private static Logger logger = Log.getLogger(Log.CONFIGURATION, UFTPStartupTask.class);

	private final Kernel kernel;

	public UFTPStartupTask(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void run() {
		try {
			Properties cfg = kernel.getContainerProperties().getRawProperties();
			if(!Boolean.parseBoolean(cfg.getProperty(UFTPProperties.PREFIX+UFTPProperties.PARAM_ENABLE_UFTP, "true")))
			{
				logger.info("UFTP is disabled.");
				return;
			}
			LogicalUFTPServer connector = new LogicalUFTPServer(kernel);
			kernel.setAttribute(LogicalUFTPServer.class, connector);
			kernel.register(connector);
		}catch(Throwable ex) {
			Log.logException("UFTP connector could not be initialised, UFTP will not be available.", ex, logger);
		}
	}

}
