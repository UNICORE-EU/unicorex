package eu.unicore.uas.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.unicore.util.Pair;

public class TestUtils {

	@Test
	public void testPair(){
		Pair<String, Integer> p1=new Pair<String,Integer>("test", 1);
		assertEquals("test",p1.getM1());
		assertEquals(Integer.valueOf(1),p1.getM2());
		
		Pair<String, Integer> p2=new Pair<String,Integer>();
		p2.setM1("test");
		p2.setM2(1);
		assertEquals("test",p2.getM1());
		assertEquals(Integer.valueOf(1),p2.getM2());
		
		assertEquals(p1,p2);
	}

	@Test(expected = RuntimeException.class)
	public void testLimitedBAOS() throws Exception {
		LimitedByteArrayOutputStream lb = new LimitedByteArrayOutputStream(16);
		lb.write(new byte[32]);
	}

}
