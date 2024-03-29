package eu.unicore.uas.metadata;

import eu.unicore.services.server.AbstractStartupTask;

/**
 * Loads configuration
 * @author K. Benedyczak
 */
public class MetadataStartupTask extends AbstractStartupTask {
	@Override
	public void run() {
		MetadataProperties props = new MetadataProperties(
				getKernel().getContainerProperties().getRawProperties());
		getKernel().addConfigurationHandler(props);
		getKernel().setAttribute(MetadataProperties.class, props);
	}

}
