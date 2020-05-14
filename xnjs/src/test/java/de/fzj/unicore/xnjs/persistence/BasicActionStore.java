/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;

/**
 * non-persistent action store using a hash map
 * 
 * @author schuller
 */
public class BasicActionStore extends AbstractActionStore {
	
	private final Map<String,Action>map=new ConcurrentHashMap<String, Action>();

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
