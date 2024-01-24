package eu.unicore.xnjs.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;

/**
 * non-persistent action store using a hash map
 * 
 * @author schuller
 */
public class BasicActionStore extends AbstractActionStore {
	
	private final Map<String,Action>map = new ConcurrentHashMap<>();

	public BasicActionStore(){
		super();
	}

	@Override
	public Collection<String> getUniqueIDs(){
		return map.keySet();
	}
	
	@Override
	public Collection<String> getActiveUniqueIDs(){
		Collection<String>ids = new HashSet<>();
		for(String id: getUniqueIDs()){
			if(ActionStatus.DONE!=map.get(id).getStatus()){
				ids.add(id);
			}
		}
		return ids;
	}

	@Override
	protected Action doGet(String id) {
		return map.get(id);
	}
	
	@Override
	protected Action tryGetForUpdate(String id) {
		return map.get(id);
	}
	
	@Override
	protected Action doGetForUpdate(String id) {
		return map.get(id);
	}

	@Override
	protected void doRemove(Action action) {
		map.remove(action.getUUID());
	}

	@Override
	protected void doStore(Action action) {
		map.put(action.getUUID(),action);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public int size(int actionStatus) {
		int i=0;
		for(Action action: map.values()){
			if(action.getStatus()==actionStatus) i++;
		}
		return i;
	}
	
	public void removeAll(){
		map.clear();
	}
}
