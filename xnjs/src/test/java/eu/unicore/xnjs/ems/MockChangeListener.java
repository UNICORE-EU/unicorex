package eu.unicore.xnjs.ems;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

@Singleton
public class MockChangeListener implements ActionStateChangeListener {

	private final Map<String, AtomicInteger> changes = new HashMap<>();
	
	@Override
	public void stateChanged(Action action, int newState) {
		getOrCreate(action.getUUID()).incrementAndGet();
	}
	
	public synchronized AtomicInteger getOrCreate(String id) {
		AtomicInteger num = changes.get(id);
		if(num == null) {
			num = new AtomicInteger();
			changes.put(id, num);
		}
		return num;
	}
	
	
}
