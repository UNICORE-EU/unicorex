package eu.unicore.uas.util;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;

/**
 * moves the target object through a set of states
 *
 * @author schuller
 */
public class StateMover<T> implements Callable<State<T>>{

	protected final Logger log = Log.getLogger(Log.UNICORE, StateMover.class);
	
	protected final State<T> initalState;
	protected final T target;
	protected State<T> currentState;
	protected boolean stop = false;
	
	protected boolean asyncMode = false;

	/**
	 * create a new state mover
	 * @param initalState - the initial state
	 * @param target - the target object
	 * @param asyncMode - whether the state machine should pause after each transition
	 */
	public StateMover(State<T> initalState, T target, boolean asyncMode){
		this.initalState = initalState;
		this.target = target;
		currentState = initalState;
		this.asyncMode = asyncMode;
	}
	
	/**
	 * create a new state mover
	 * @param initalState - the initial state
	 * @param target - the target object
	 */
	public StateMover(State<T> initalState, T target){
		this(initalState,target,false);
	}

	public StateMover(T target, boolean asyncMode){
		this(new AnnotationsStateMachine<T>(null,target).getInitialState(), target, asyncMode);
	}
	
	/**
	 * create a new state mover based on annotations
	 * @param target - the target object
	 */
	public StateMover(T target){
		this(target, false);
	}
	
	/**
	 * run the state machine, beginning with the initial state <br/>
	 * @return the end state or <code>null</code> if no end state
	 */
	@Override
	public State<T> call() throws Exception {
		do{
			boolean pausable = currentState.isPausable();
			int retryCount = currentState.getNumberOfRetries();
			int retryDelay = currentState.getRetryDelay();
			int attempts = 0;
			try{
				if(log.isDebugEnabled()){
					log.debug("Processing state "+currentState.getName()
							+" on "+target.getClass().getName());
				}
				boolean retry = true;
				while(retry){
					retry = attempts < retryCount;
					attempts++;	
					try{
						currentState = currentState.next(target);
						retry=false;
					}catch(Exception ex){
						if(!retry){
							throw ex;
						}
						else{
							if (log.isDebugEnabled()){
								log.debug("Error: "+Log.getDetailMessage(ex)
										+" in state "+currentState.getName()
										+" on "+target.getClass().getName()
										+ ", will retry in "+retryDelay+ " ms.");
							}
							Thread.sleep(retryDelay);
						}
					}
				}
				
				if(log.isDebugEnabled() && currentState!=null){
					log.debug("Entering state "+currentState.getName()
							+" on "+target.getClass().getName());
				}
			}
			catch(Exception ex){
				if(log.isDebugEnabled()){
					log.debug("Handling error in state "+currentState.getName()
							+" on "+target.getClass().getName());
				}
				
				currentState = currentState.onError(target,ex);
				
				if(log.isDebugEnabled() && currentState!=null){
					log.debug("Entering state "+currentState.getName()
							+" on "+target.getClass().getName());
				}
			}
			
			
			if(asyncMode){
				stop = pausable;
			}
			else{
				stop = currentState == null;
			}
		}while(!stop);

		return currentState;
	}

	public State<T> getCurrentState() {
		return currentState;
	}

	public boolean isAsyncMode() {
		return asyncMode;
	}

	public void setAsyncMode(boolean asyncMode) {
		this.asyncMode = asyncMode;
	}

	
	
}
