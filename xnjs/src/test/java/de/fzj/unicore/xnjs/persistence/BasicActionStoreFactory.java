package de.fzj.unicore.xnjs.persistence;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import de.fzj.unicore.xnjs.XNJS;

@Singleton
public class BasicActionStoreFactory implements IActionStoreFactory{
	
	private static Map<String, IActionStore>map = new HashMap<>();
	
	public synchronized IActionStore getInstance(String id, XNJS config) {
		IActionStore as=map.get(id);
		if(as==null){
			as=new BasicActionStore();
			as.setName(id);
			map.put(id, as);
		}
		return as;
	}

	
}
