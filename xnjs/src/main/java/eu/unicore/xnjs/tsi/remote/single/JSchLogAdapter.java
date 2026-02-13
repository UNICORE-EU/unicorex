package eu.unicore.xnjs.tsi.remote.single;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Get JSch to log via Log4j
 */
public class JSchLogAdapter implements com.jcraft.jsch.Logger {

	private static final Logger logger = eu.unicore.util.Log.getLogger("unicore.xnjs.tsi.ssh");

	@Override
	public boolean isEnabled(int level) {
		return logger.isEnabled(getLevel(level));
	}

	@Override
	public void log(int level, String message) {
		logger.log(getLevel(level), message);
	}

	static Level getLevel(int level) {
		switch (level) {
		case com.jcraft.jsch.Logger.DEBUG:
			return Level.DEBUG;
		case com.jcraft.jsch.Logger.INFO:
			return Level.INFO;
		case com.jcraft.jsch.Logger.WARN:
			return Level.WARN;
		case com.jcraft.jsch.Logger.ERROR:
			return Level.ERROR;
		case com.jcraft.jsch.Logger.FATAL:
			return Level.FATAL;
		default:
			return Level.TRACE;
		}
	}
}