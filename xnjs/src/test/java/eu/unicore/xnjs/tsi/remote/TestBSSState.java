package eu.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TestBSSState {

	@Test
	public void testParseProcessList() throws Exception {
		String processList = FileUtils.readFileToString(
				new File("src/test/resources/ps-test.txt"), "UTF-8");
		String tsiNode = "localhost";
		assert processList.contains("  935 ");
		assert processList.contains(" 1234 ");
		assert processList.contains("64738 ");
		Set<String>ps = BSSState.parseTSIProcessList(processList, tsiNode);
		
		assertTrue(ps.contains("INTERACTIVE_localhost_935"));
		assertTrue(ps.contains("INTERACTIVE_localhost_1234"));
		assertTrue(ps.contains("INTERACTIVE_localhost_64738"));
	}

}
