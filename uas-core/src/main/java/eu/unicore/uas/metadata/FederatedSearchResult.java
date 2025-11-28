package eu.unicore.uas.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Federated search result for one storage
 * 
 * @author Konstantine Muradov
 */
public class FederatedSearchResult implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<String> resourceURLs = new ArrayList<>();

	public List<String> getResourceURLs()
	{
		return resourceURLs;
	}

	public void addResourceURL(String resourceURL)
	{
		if(resourceURL != null && !resourceURL.isEmpty())
		{
			resourceURLs.add(resourceURL);
		}
	}

	public void addResourceURLs(List<String> resourceURLs)
	{
		this.resourceURLs.addAll(resourceURLs);
	}

	public int getResourceCount()
	{
		return resourceURLs.size();
	}
}