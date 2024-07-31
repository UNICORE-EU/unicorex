package eu.unicore.uas.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestPropertyHelper {

	@Test
	public void test1(){
	
		HashMap<String,String>props=new HashMap<String, String>();
		props.put("TEST_1","foo1");
		props.put("TEST_2","foo2");
		props.put("TEST_group_1","gr1");
		props.put("TEST__group_2","gr2");
		props.put("NOTME_xx","abc");
		
		
		for(int i=0;i<200;i++){
			props.put("XX_"+i,"xx"+1);
		}
		PropertyHelper ah=new PropertyHelper(props, "TEST_");
		Iterator<String>i=ah.keys();
		int c=0;
		while(i.hasNext() && c < props.size()*2){
			c++;
			assertTrue(i.next().startsWith("TEST_"));
		}
		assertEquals(4,c);
		
		Map<String,String>filteredProps=ah.getFilteredMap();
		assertEquals(4,filteredProps.size());
		
		filteredProps=ah.getFilteredMap("group");
		assertEquals(2,filteredProps.size());
		Iterator<String>i2=filteredProps.keySet().iterator();
		while(i2.hasNext()){
			assertTrue(i2.next().contains("group"));
		}
		
	}
	
}
