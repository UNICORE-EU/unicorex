package de.fzj.unicore.uas.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.NumberFormat;
import java.util.Date;

import org.junit.Test;

public class TestUnitParser {

	@Test
	public void testPattern1(){
		UnitParser p=new UnitParser(null,null);
		String v="1 m";
		assertEquals("1",p.getNumber(v));
		assertEquals("m",p.getUnits(v));

		String v2=" 1213K";
		assertEquals("1213",p.getNumber(v2));
		assertEquals("K",p.getUnits(v2));

		String v3=" 1213.0  Megs ";
		assertEquals("1213.0",p.getNumber(v3));
		assertEquals("Megs",p.getUnits(v3));

		String v4=" 1213,0  Megs ";
		assertEquals("1213,0",p.getNumber(v4));
		assertEquals("Megs",p.getUnits(v4));
	}

	@Test
	public void testGetConversionFactors(){
		UnitParser p=new UnitParser(
				new String[][]{
						{"a"},{"b","B"},{"c","c2","c3"}
				},new int[]{10,10});

		assertEquals(1,p.getConversionFactor("a"));
		assertEquals(10,p.getConversionFactor("b"));
		assertEquals(10,p.getConversionFactor("B"));
		assertEquals(100,p.getConversionFactor("c3"));
	}

	@Test
	public void testGetStringValue(){
		UnitParser p=new UnitParser(
				new String[][]{
						{"a"},{"b","B"},{"c","c2","c3"}
				},new int[]{10,10}, 0);

		assertEquals("1",p.getStringValue("1 a"));
		assertEquals("10",p.getStringValue("1B"));
		assertEquals("200",p.getStringValue("2c3"));
	}
	
	@Test
	public void testGetDoubleValue(){
		UnitParser p=new UnitParser(
				new String[][]{
						{"a"},{"b","B"},{"c","c2","c3"}
				},new int[]{10,10}, 0);

		assertEquals(1.0,p.getDoubleValue("1 a"),0.1);
		assertEquals(10.0,p.getDoubleValue("1B"),0.1);
		assertEquals(200.0,p.getDoubleValue("2c3"),0.1);
	}	
	
	@Test
	public void testGetHumanReadable(){
		UnitParser p=new UnitParser(
				new String[][]{
						{"a"},{"b"},{"c"}
				},new int[]{10,10}, 1);
		double t1=11;
		NumberFormat nf=NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(3);
		assertEquals(nf.format(1.1)+"b",p.getHumanReadable(t1));
		
		double t2=1211;
		assertEquals(nf.format(12.1)+"c",p.getHumanReadable(t2));
		
	}

	@Test
	public void testCapacityUnits(){
		UnitParser p=UnitParser.getCapacitiesParser(1);
		p.setMinimumDigits(1);
		
		NumberFormat nf=NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(1);
		nf.setMinimumFractionDigits(1);
		
		double d1=1024.0;
		assertEquals(nf.format(1)+"K", p.getHumanReadable(d1));
		
		double d2=256*1024.0;
		assertEquals(nf.format(256)+"K", p.getHumanReadable(d2));
		
		double d3=256*1024*1024.0;
		assertEquals(nf.format(256)+"M", p.getHumanReadable(d3));
		
		assertEquals(1024000.0, p.getDoubleValue("1000K"),0.1);
		assertEquals(1024*1024*1000.0, p.getDoubleValue("1000M"),0.1);
		assertEquals(1024*1024*1024*1000.0, p.getDoubleValue("1000G"),0.1);	
		assertEquals(1024*1000.0, p.getDoubleValue("1024000"),0.1);	
		
	}
	
	@Test
	public void testBigNumbers() {
		UnitParser p=UnitParser.getCapacitiesParser(1);
		p.setMinimumDigits(1);
		System.out.println(p.getHumanReadable(p.getDoubleValue("1500G")));
	}
	

	@Test
	public void testTimeUnits(){
		UnitParser p=UnitParser.getTimeParser(1);
		p.setMinimumDigits(1);
		
		NumberFormat nf=NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(1);
		nf.setMinimumFractionDigits(1);
		
		double d1=60.0;
		assertEquals(nf.format(1)+"min", p.getHumanReadable(d1));
		
		assertEquals(3600.0, p.getDoubleValue("1h"),0.1);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testDateParsing()throws Exception{
		String d1="13:22";
		Date date=UnitParser.extractDateTime(d1);
		assertEquals(date.getYear(), new Date().getYear());
		System.out.println(date);
	}


	//check that a hh:mm date "before" now is interpreted as "tomorrow"
	@SuppressWarnings("deprecation")
	@Test
	public void testWrapDate()throws Exception{
		Date d=new Date(System.currentTimeMillis()-3600*1000);
		int hours=d.getHours();
		int minutes=d.getMinutes();
		String d1=hours+":"+minutes;
		Date date=UnitParser.extractDateTime(d1);
		//check that the day has been changed
		assertTrue(date.getDate()!=new Date().getDate());
	}
	
}
