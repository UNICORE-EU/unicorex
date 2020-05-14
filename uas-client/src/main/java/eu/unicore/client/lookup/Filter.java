package eu.unicore.client.lookup;

import eu.unicore.client.core.BaseServiceClient;

public interface Filter {

	/**
	 * test whether the given resource matches
	 * 
	 * @param resource - the resource to test
	 * @return true if the resource passes the filter, false otherwise
	 */
	public boolean accept(BaseServiceClient resource) throws Exception;

}

