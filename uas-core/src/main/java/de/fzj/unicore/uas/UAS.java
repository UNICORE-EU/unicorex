/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package de.fzj.unicore.uas;

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
	
	/**
	 * If one of the following is used as protocol for file transfers,
	 * UNICORE should automatically try to negotiate a suitable file transfer protocol.
	 */
	public static final String[] AUTO_NEGOTIATE_FT_PROTOCOL = {"auto"};

	private UASProperties uasProperties;

	/**
	 * @param configFile
	 */
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
		kernel.addConfigurationHandler(UASProperties.class, uasProperties);
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
