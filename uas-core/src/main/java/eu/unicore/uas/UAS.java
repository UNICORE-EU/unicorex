package eu.unicore.uas;

import java.util.Properties;

import eu.unicore.services.USEContainer;
import eu.unicore.util.Log;

/**
 * Main UNICORE/X class, used to launch the container
 *
 * @author schuller
 */
public class UAS extends USEContainer {
	// service names
	public static final String TSF = "TargetSystemFactoryService";
	public static final String TSS = "TargetSystemService";
	public static final String JMS = "JobManagement";
	public static final String RESERVATIONS = "ReservationManagement";
	public static final String SMS = "StorageManagement";
	public static final String SMF = "StorageFactory";
	public static final String META = "MetadataManagement";
	public static final String SERVER_FTS = "ServerServerFileTransfer";
	public static final String CLIENT_FTS = "ClientServerFileTransfer";
	public static final String TASK = "Task";

	private UASProperties uasProperties;

	public UAS(String configFile) throws Exception {
		super(configFile, "UNICORE/X");
		initCommon();
	}

	/**
	 * @param properties - Server configuration
	 */
	public UAS(Properties properties) throws Exception {
		super(properties, "UNICORE/X");
		initCommon();
	}

	public String getVersion() {
		return getClass().getPackage().getSpecificationVersion()!=null?
				getClass().getPackage().getSpecificationVersion() : "DEVELOPMENT";
	}

	private void initCommon() throws Exception {
		this.uasProperties = new UASProperties(kernel.getContainerProperties().getRawProperties());
		kernel.addConfigurationHandler(uasProperties);
		kernel.setAttribute(UASProperties.class, uasProperties);
	}

	public static void main(String[] args) throws Exception {
		try{
			System.out.println("Reading config from " + args[0]);
			UAS uas=new UAS(args[0]);
			uas.startSynchronous();
		}catch(Throwable ex){
			String msg = Log.createFaultMessage("ERROR during server startup, server NOT started", ex);
			Log.getLogger("unicore", UAS.class).fatal(msg);
			ex.printStackTrace();
			System.err.println(msg);
			System.exit(1);
		}
	}
}
