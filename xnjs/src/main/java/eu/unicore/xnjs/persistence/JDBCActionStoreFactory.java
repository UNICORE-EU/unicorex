package eu.unicore.xnjs.persistence;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.xnjs.XNJS;
import jakarta.inject.Singleton;

/**
 * Generic persistence engine creation. <br/>
 * Expects the class name of a class implementing {@link IActionStore} in a property
 * <code>JDBCActionStore.class</code>
 * 
 * @author schuller
 */
@Singleton
public class JDBCActionStoreFactory implements IActionStoreFactory{

	private final Map<String,JDBCActionStore> map = new HashMap<>();

	public synchronized IActionStore getInstance(String id, XNJS config) throws Exception {
		JDBCActionStore m=map.get(id);
		if(m==null){
			m=config.get(JDBCActionStore.class);
			m.setName(id);
			m.start();
			map.put(id,m);
		}
		return m;
	}
	
	void remove(JDBCActionStore store){
		map.remove(store.getName());
	}
}
