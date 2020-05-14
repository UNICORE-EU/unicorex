package eu.unicore.client;

import org.junit.Test;

public class TestJob {

	@Test
	public void TestJobBuilder() throws Exception {
		Job j = new Job();
		j.application("Date", "1.0");
		j.arguments("-rfc","-v");
		j.environment("FOO", "bar").parameter("SOURCE", "./input.sh");
		j.stagein().from("http://google.de").to("index.html").ignore_error();
		j.stagein().from("https://myserver.com/1").to("data1.txt").ignore_error().with_credentials().token("123");
		j.stagein().from("https://myserver.com/2").to("data2.txt").with_credentials().username("foo", "123");
		j.stagein().data("this is some test data").to("data3.txt");
		j.resources().nodes(16).runtime("4h");
		System.out.println(j.getJSON().toString(2));
	}
	
}
