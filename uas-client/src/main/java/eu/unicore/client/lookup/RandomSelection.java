package eu.unicore.client.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.core.SiteClient;

/**
 * randomly choose a site
 */
public class RandomSelection implements SiteSelectionStrategy{

	private final static Random random = new Random();
	
	private final Map<String, AtomicInteger>selected = new HashMap<>();
	
	public SiteClient select(List<SiteClient> available) {
		SiteClient selectedTSS = available.get(random.nextInt(available.size()));
		try{
			String name = selectedTSS.getEndpoint().getUrl();
			synchronized(this){
				AtomicInteger val = selected.get(name);
				if(val==null) {
					val = new AtomicInteger();
					selected.put(name, val);
				}
				val.incrementAndGet();
			}
		}
		catch(Exception ex){}
		return selectedTSS;
	}
	
	public Map<String,AtomicInteger>getSelectionStatistics(){
		return selected;
	}
}