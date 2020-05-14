package de.fzj.unicore.xnjs.ems;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.persist.util.Wrapper;

/**
 * Processing context for an {@link Action}<br/>
 *
 * @author schuller
 */
public class ProcessingContext implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<String,Wrapper<Serializable>>contents=new HashMap<String, Wrapper<Serializable>>();
	
	public <T> T getAs(String key, Class<T>clazz){
		Wrapper<?> wrapper=contents.get(key);
		if(wrapper==null)return null;
		Serializable val = wrapper.get();
		if(clazz.isAssignableFrom(val.getClass())){
			return clazz.cast(val);
		}
		else throw new IllegalArgumentException("Object found in map, but has wrong type. " +
				"Found <"+val.getClass().getName()+"> expected <"+clazz.getName());
		
	}
	
	public Serializable get(String key){
		Wrapper<?> wrapper=contents.get(key);
		if(wrapper==null)return null;
		return wrapper.get();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> key){
		return (T)get(key.getName());
	}

	public void set(Object content){
		put(content.getClass().getName(),content);
	}
	
	public void remove(Object content){
		if(content instanceof Class){
			remove(((Class<?>)content).getName());
		}
		else{
			remove(content.getClass().getName());
		}
	}
	
	public void remove(String key){
		contents.remove(key);
	}
	
	public void put(String key, Object content){
		if(!(content instanceof Serializable)){
			throw new IllegalArgumentException("Not serializable : "+content.getClass());
		}
		contents.put(key, new Wrapper<Serializable>((Serializable)content));
	}
	
	public void put(String key, Serializable content){
		contents.put(key, new Wrapper<Serializable>(content));
	}

	public void put(Class<?>key, Serializable content){
		contents.put(key.getName(), new Wrapper<Serializable>(content));
	}

}
