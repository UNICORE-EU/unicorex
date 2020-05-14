package de.fzj.unicore.uas.metadata;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.unigrids.x2006.x04.services.metadata.FederatedSearchResultCollectionDocument;

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
	
	public FederatedSearchResultCollectionDocument getTypeOfDocument()
	{
		FederatedSearchResultCollectionDocument result = FederatedSearchResultCollectionDocument.Factory
				.newInstance();
		 
		result.addNewFederatedSearchResultCollection();
		result.getFederatedSearchResultCollection().setSearchEndTime(DateToCalendar(getSearchStartTime()));
		result.getFederatedSearchResultCollection().setSearchEndTime(DateToCalendar(getSearchEndTime()));
		result.getFederatedSearchResultCollection().setResourceCount(BigInteger.valueOf(getResourceCount())); 
		result.getFederatedSearchResultCollection().setStorageCount(BigInteger.valueOf(getStorageCount())); 
		
		
		List<FederatedSearchResult> searchResults = getSearchResults();
		
		for(FederatedSearchResult searchResult : searchResults)
		{
			org.unigrids.x2006.x04.services.metadata.FederatedSearchResultDocument.FederatedSearchResult searchResultElement=result.getFederatedSearchResultCollection().addNewFederatedSearchResults().addNewFederatedSearchResult();
			searchResultElement.setStorageURL(searchResult.getStorageURL());
			List<String> elementResourceNames = searchResult.getResourceNames();
			String[] elementResourceNamesArray = elementResourceNames.toArray(new String[elementResourceNames.size()]);
			searchResultElement.setResourceNameArray(elementResourceNamesArray);
		}
		
		return result;
	}
	
	public Calendar DateToCalendar(Date date){ 
		  System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" + date);	
		  Calendar cal = Calendar.getInstance();
		  cal.setTime(date);
		  return cal;
		}

}
