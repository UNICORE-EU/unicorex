package eu.unicore.uas.metadata;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Federated search results repository
 * 
 * @author Konstantine Muradov
 */
public class FederatedSearchResultCollection {
	Date searchStartTime;
	Date searchEndTime;
	ArrayList<FederatedSearchResult> items;

	public FederatedSearchResultCollection() {
		searchStartTime = new Date();
		items = new ArrayList<FederatedSearchResult>();
	}

	public Date getSearchStartTime() {
		return searchStartTime;
	}

	public Date getSearchEndTime() {
		return searchEndTime;
	}

	public void setSearchEndTime(Date endTime) {
		if (searchStartTime.after(endTime)) {
			throw new IllegalArgumentException(
					"The specified search end time is not valid - it's less than start time.");
		}

		searchEndTime = endTime;
	}

	public int getStorageCount() {
		return items.size();
	}

	public int getResourceCount() {
		int result = 0;

		for (FederatedSearchResult item : items)
			result += item.getResourceCount();

		return result;
	}

	public List<FederatedSearchResult> getSearchResults() {
		return items;
	}
	
	public void addSearchResult(FederatedSearchResult searchResult)
	{
		items.add(searchResult);
	}
	
	public void addSearchResultsRange(List<FederatedSearchResult> searchResults)
	{
		items.addAll(searchResults);
	}
	
	public Map<String,String> asMap()
	{
		Map<String,String> result = new HashMap<>();
		result.put("resourceCount", String.valueOf(getResourceCount())); 
		result.put("storageCount", String.valueOf(getStorageCount())); 
		
		List<FederatedSearchResult> searchResults = getSearchResults();
		int i = 1;
		for(FederatedSearchResult searchResult : searchResults)
		{
			for(String url: searchResult.getResourceURLs()) {
				result.put("search-result-"+i, url);
				i++;
			}
		}
		
		return result;
	}

}
