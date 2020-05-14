package de.fzj.unicore.uas.metadata;

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

	private String storageURL;
	
	private List<String> resourceNames;
	
	public FederatedSearchResult()
	{
		resourceNames = new ArrayList<String>();
	}
	
	public String getStorageURL()
	{
		return storageURL;
	}
	
	public void setStorageURL(String URL)
	{
		storageURL = URL;
	}
	
	public List<String> getResourceNames()
	{
		return resourceNames;
	}
	
	public void addResourceName(String resourceName)
	{
		if(resourceName == null || resourceName.isEmpty())
		{
			//TODO: or may be throw some exception
			return;
		}
		
		resourceNames.add(resourceName);
	}
	
	public void addResourceNames(List<String> resourceNames)
	{
		this.resourceNames.addAll(resourceNames);
	}
	
	public int getResourceCount()
	{
		return resourceNames.size();
	}
}
