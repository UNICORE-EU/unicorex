package de.fzj.unicore.uas.util;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.junit.Test;

import de.fzj.unicore.wsrflite.Capabilities;
import de.fzj.unicore.wsrflite.Capability;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import de.fzj.unicore.xnjs.io.IOCapabilities;

public class TestCapabilityFinder {

	@Test
	public void testLoadCapabilities(){
		System.out.println("Loading USE capabilities.");
		ServiceLoader<Capabilities> sl=ServiceLoader.load(Capabilities.class);
		Iterator<Capabilities>iter=sl.iterator();
		int i=0;
		while(iter.hasNext()){
			Capability[]cs=iter.next().getCapabilities();
			for(int j=0; j<cs.length;j++){
				Capability c=cs[j];
				System.out.println(c.getInterface().getName()+ " provided by "+c.getImplementation().getName());
			}
			i++;
		}
		assertTrue(0<i);
	}
	
	@Test
	public void testLoadIOCapabilities(){
		System.out.println("Loading XNJS IO capabilities.");
		ServiceLoader<IOCapabilities> sl=ServiceLoader.load(IOCapabilities.class);
		Iterator<IOCapabilities>iter=sl.iterator();
		int i=0;
		while(iter.hasNext()){
			Class<? extends IFileTransferCreator>[]cs=iter.next().getFileTransferCreators();
			for(int j=0; j<cs.length;j++){
				Class <? extends IFileTransferCreator>c=cs[j];
				System.out.println("Filetransfer creator class "+c.getName());
			}
			i++;
		}
		assertTrue(0<i);
	}
	
}
