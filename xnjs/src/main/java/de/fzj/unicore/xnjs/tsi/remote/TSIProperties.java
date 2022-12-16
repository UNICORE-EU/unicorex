package de.fzj.unicore.xnjs.tsi.remote;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * config properties for connecting to a UNICORE TSI server
 *
 * @author schuller
 */
@Singleton
public class TSIProperties extends PropertiesHelper {

	private static final Logger logger=Log.getLogger(Log.CONFIGURATION, TSIProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX="CLASSICTSI.";

	// TSI Connection stuff
	public static final String TSI_MACHINE="machine";
	public static final String TSI_PORT="port";
	public static final String TSI_MYPORT="replyport";
	public static final String TSI_BSSUSER="priveduser";
	public static final String TSI_TIMEOUT="socket.timeout";
	public static final String TSI_NO_CHECK="socket.no_check_matching_ips";
	public static final String TSI_CONNECT_TIMEOUT="socket.connect.timeout";
	public static final String TSI_DISABLE_SSL="ssl.disable";
	public static final String TSI_WORKER_LIMIT ="limitTSIConnections";
	public static final String TSI_POOL_SIZE ="pooledTSIConnections";
	
	// TSI commands
	public static final String TSI_CD="CD";
	public static final String TSI_CP="CP";
	public static final String TSI_LN="LN";
	public static final String TSI_MV="MV";
	public static final String TSI_RM="RM";
	public static final String TSI_RMDIR="RMDIR";
	public static final String TSI_MKDIR="MKDIR";
	public static final String TSI_CHMOD="CHMOD";
	public static final String TSI_CHGRP="CHGRP";
	public static final String TSI_GROUPS="GROUPS";
	public static final String TSI_UMASK="UMASK";
	public static final String TSI_KILL="KILL";
	public static final String TSI_BUFFERSIZE="BUFFERSIZE";
	
	// various
	public static final String BSS_UPDATE_INTERVAL="statusupdate.interval";
	public static final String BSS_MAX_RUNTIME_FOR_INTERACTIVE_APPS="interactive_execution.maxtime";
	public static final String BSS_NO_USER_INTERACTIVE_APPS="interactive_execution.disable";
	public static final String BSS_PS="PS";
	
	public static final String TSI_FILESYSTEM_ID="FSID";

	/**
	 * TODO add property for defining directly as milliseconds...
	 */
	public static final String BSS_GRACE_PERIOD="statusupdate.grace";

	public static final String RES_ENABLED="reservationEnabled";
	
	public static final String RES_ADMIN_USER="reservationAdminUser";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();

	static
	{
		// connection settings
		
		META.put(TSI_MACHINE, new PropertyMD("localhost").
				setDescription("TSI host(s) or IP address(es). Specify multiple hosts in the format 'machine1[:port1],machine2[:port2],...'"));
		META.put(TSI_PORT, new PropertyMD("4433").setInt().setPositive().
				setDescription("TSI port to connect to."));
		META.put(TSI_MYPORT, new PropertyMD("7654").setInt().setPositive().
				setDescription("Reply port on UNICORE/X server."));
		META.put(TSI_TIMEOUT, new PropertyMD("180").setInt().setBounds(0, Integer.MAX_VALUE).
				setDescription("Read timeout (seconds) on the TSI connection. Set to '0' for no timeout."));	
		META.put(TSI_CONNECT_TIMEOUT, new PropertyMD("10").setInt().setBounds(0, Integer.MAX_VALUE).
				setDescription("Connection timeout (seconds) on when establishing (or checking) the TSI connection. Set to '0' for no timeout."));	
		
		META.put(TSI_NO_CHECK, new PropertyMD("false").
				setDescription("Disable checking if IP address(es) of command/data socket callbacks are as expected."));
	
		META.put(TSI_BSSUSER, new PropertyMD("unicore").
				setDescription("Account used for getting statuses of all batch jobs (cannot be 'root')."));	
		META.put(TSI_DISABLE_SSL, new PropertyMD("true").setBoolean().
				setDescription("Whether to disable SSL for the TSI-UNICORE/X connection."));
		META.put(TSI_WORKER_LIMIT, new PropertyMD("-1").setInt().
				setDescription("Limit the total number of TSI worker processes created by this UNICORE/X ('-1' means no limit)."));
		META.put(TSI_POOL_SIZE, new PropertyMD("4").setInt().setPositive().
				setDescription("How many TSI worker processes per TSI host to keep (even if idle)."));
		
		// commands
		META.put(TSI_CD, new PropertyMD("cd").
				setDescription("Unix 'cd' command."));
		META.put(TSI_CP, new PropertyMD("/bin/cp").
				setDescription("Unix 'cp' command."));
		META.put(TSI_LN, new PropertyMD("/bin/ln -s").
				setDescription("Unix 'ln' command."));
		META.put(TSI_MV, new PropertyMD("/bin/mv").
				setDescription("Unix 'mv' command."));
		META.put(TSI_RM, new PropertyMD("/bin/rm").
				setDescription("Unix 'rm' command."));
		META.put(TSI_RMDIR, new PropertyMD("/bin/rm -rf").
				setDescription("Unix directory removal command."));
		META.put(TSI_MKDIR, new PropertyMD("/bin/mkdir -p").
				setDescription("Unix directory creation command."));
		META.put(TSI_CHMOD, new PropertyMD("/bin/chmod").
				setDescription("Unix 'chmod' command."));
		META.put(TSI_CHGRP, new PropertyMD("/bin/chgrp").
				setDescription("Unix 'chgrp' command."));
		META.put(TSI_GROUPS, new PropertyMD("groups").
				setDescription("Unix 'groups' command."));
		META.put(TSI_UMASK, new PropertyMD("umask").
				setDescription("Unix 'umask' command."));
		META.put(TSI_KILL, new PropertyMD(
				"SID=$(ps -e -osid,pid | egrep -o \"^\\s*[0-9]+ \\s*[PID]\" "
				+ "| egrep -o \"^\\s*[0-9]+\"); pkill -s $SID").
				setDescription("Unix command template for aborting a process and its child processes."));
		META.put(TSI_BUFFERSIZE, new PropertyMD(String.valueOf(1024*1024)).setInt().setPositive().
				setDescription("Buffer size (in bytes) for transferring data from/to the TSI."));
		
		// various
		META.put(BSS_UPDATE_INTERVAL, new PropertyMD("10000").setInt().setPositive().
				setDescription("Interval (ms) for updating job statuses on the batch system."));
		META.put(BSS_NO_USER_INTERACTIVE_APPS, new PropertyMD("false").setBoolean().
				setDescription("Disable execution of user commands on the TSI node."));
		META.put(BSS_PS, new PropertyMD("ps -e").setDeprecated().
				setDescription("deprecated"));
		META.put(BSS_MAX_RUNTIME_FOR_INTERACTIVE_APPS, new PropertyMD("-1").setInt().setDeprecated().
				setDescription("(deprecated)"));
		META.put(BSS_GRACE_PERIOD, new PropertyMD("2").setInt().setPositive().
				setDescription("How many times the XNJS will re-check job status in case of a 'lost' job."));
		META.put(TSI_FILESYSTEM_ID, new PropertyMD().setDescription("TSI filesystem identifier which "
				+ "uniquely identifies the file system. "
						+ "The default value uses the '" + PREFIX + "machine' property."));
		META.put(RES_ADMIN_USER, new PropertyMD("unicore").
				setDescription("Account used for making reservations (cannot be 'root'). If null, the current user's login will be used."));
		META.put(RES_ENABLED, new PropertyMD("false").setBoolean().
				setDescription("Whether to enable the reservation interface."));
	}

	public TSIProperties()
			throws ConfigurationException {
		this(new Properties());
	}

	public TSIProperties(Properties properties)
			throws ConfigurationException {
		super(PREFIX, properties, META, logger);
	}

	public String getTSIMachine(){
		return getValue(TSI_MACHINE);
	}
	
	public int getTSIPort(){
		return getIntValue(TSI_PORT);
	}
	
	public int getTSIMyPort(){
		return getIntValue(TSI_MYPORT);
	}
	
	public String getBSSUser(){
		return getValue(TSI_BSSUSER);
	}
	

	@Override
	protected void findUnknown(Properties toCheck) {
		String[]toRemove = new String[]{
			"TSI_LS","TSI_DF",
		};
		for (String r : toRemove)
		{
			if (toCheck.containsKey(prefix + r)){
				log.warn("The setting <" + prefix + r 
						+ "> is deprecated and will be ignored. "
						+ "Please remove it from configuration.");
				toCheck.remove(prefix + r);
			}
		}
		super.findUnknown(toCheck);
	}
	
}
