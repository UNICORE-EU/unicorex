package eu.unicore.uas.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnnotationsStateMachine<T> {

	private final T target;
	
	private String initialState;
	
	/**
	 * create a state machine
	 * @param initialState - if <code>null</code>, the initial state will be 
	 *                       determined by the annotations
	 * @param target - the target object
	 */
	public AnnotationsStateMachine(String initialState, T target){
		this.target = target;
		this.initialState = initialState;
	}

	public State<T> getInitialState() {
		return new StateImpl<T>(initialState, target);
	}

	/**
	 * use reflection to read the OnState annotation and 
	 * find the correct methods to invoke
	 *
	 * @author schuller
	 */
	public static class StateImpl<T> implements State<T>{

		private String name;
		private Method method;
		private StateInfo annotation;
		
		/**
		 * 
		 * @param state - the state name. If this is <code>null</code>, the initial state will be searched and used
		 * @param target - the target object
		 * @throws IllegalStateException
		 */
		public StateImpl(String state, T target)throws IllegalStateException{
			this.name=state;
			findMethod(target.getClass());
		}

		private void findMethod(Class<?>spec) throws IllegalStateException{
			try{
				Method[] methods=spec.getDeclaredMethods();
				for(Method m:methods){
					StateInfo meta=m.getAnnotation(StateInfo.class);
					if(meta!=null){
						// if name is given, find by name, else find 
						// the marked initial state
						String stateName=meta.name();
						boolean found=false;
						
						if(name!=null){
							found = name.equals(stateName);
						}
						else{
							found = meta.isInitial();
							if(found){
								name = meta.name();
							}
						}
						
						if(found){
							method=m;
							annotation=meta;
							break;
						}
					}
				}
				if(method==null && !spec.getSuperclass().equals(Object.class)){
					findMethod(spec.getSuperclass());
				}
			}
			catch(Exception ex){
				throw new IllegalStateException(ex);
			}
			
			if(method==null){
				throw new IllegalStateException("No method found for state <"+name+">");
			}
		}

		@Override
		public State<T> next(T target) throws Exception {
			try{
				method.invoke(target, (Object[])null);
			}catch(InvocationTargetException ite){
				if(ite.getTargetException() instanceof Exception){
					throw (Exception)ite.getTargetException();
				}
				else if(ite.getTargetException() instanceof Error){
					throw (Error)ite.getTargetException();
				}
				else {
					throw ite;
				}
			}
			if(annotation.isTerminal()){
				return null;
			}
			else{
				String nextState=annotation.onSuccess();
				return new StateImpl<T>(nextState, target);
			}
		}

		@Override
		public State<T> onError(T target, Exception ex) throws Exception {
			String errorState=annotation.onError();
			if(errorState.isEmpty()){
				return null;
			}
			else{
				return new StateImpl<T>(errorState, target);
			}
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isPausable(){
			return annotation.isPausable();
		}
		
		@Override
		public int getNumberOfRetries(){
			return annotation.retryCount();
		}
		
		@Override
		public int getRetryDelay(){
			return annotation.retryDelay();
		}
	}

}
