package eu.unicore.client.lookup;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.core.SiteClient;

/**
 * chooses a site from a list of available ones
 * 
 * @author schuller
 */
public interface SiteSelectionStrategy {

	public SiteClient select(List<SiteClient>available);
	
	public Map<String,AtomicInteger>getSelectionStatistics();
	
}
