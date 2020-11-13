package de.fzj.unicore.uas.admin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestShowUsageOverview {

	@Test
	@SuppressWarnings("unchecked")
	public void testMerge(){
		List<String>serviceNames=Arrays.asList(new String[]{"foo","bar"});
		Map<String, AtomicInteger>fooInst=new HashMap<String, AtomicInteger>();
		fooInst.put("Alice", new AtomicInteger(3));
		fooInst.put("Bob", new AtomicInteger(10));
		Map<String, AtomicInteger>barInst=new HashMap<String, AtomicInteger>();
		barInst.put("Alice", new AtomicInteger(1));
		barInst.put("Bob", new AtomicInteger(0));
		Map<String,Map<String,Integer>>merged=new ShowServerUsageOverview().merge(null, serviceNames, fooInst, barInst);
		Assert.assertEquals(2, merged.size());
		Assert.assertNotNull(merged.get("Alice"));
		Assert.assertEquals(2,merged.get("Alice").size());
		Assert.assertNotNull(merged.get("Bob"));
		Assert.assertEquals(2,merged.get("Bob").size());
		System.out.println(merged);
		Assert.assertEquals((Integer)10,merged.get("Bob").get("foo"));
		Assert.assertEquals((Integer)0,merged.get("Bob").get("bar"));
	}
	
}
