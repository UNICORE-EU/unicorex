package eu.unicore.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.client.core.JobClient.Status;

public class TestJob {

	@Test
	public void testJobBuilder() throws Exception {
		Job j = new Job();
		j.tags("test", "foo");
		j.type(Job.Type.BATCH);
		j.pre_command("unzip data.zip");
		j.application("Date", "1.0");
		j.arguments("-rfc","-v");
		j.environment("FOO", "bar").parameter("SOURCE", "./input.sh");
		j.stagein().from("http://google.de").to("index.html").ignore_error();
		j.stagein().from("https://myserver.com/1").to("data1.txt")
			.ignore_error()
			.with_credentials().bearerToken("123");
		j.stagein().from("https://myserver.com/2").to("data2.txt").with_credentials().username("foo", "123");
		j.stagein().data("this is some test data").to("data.zip");
		j.resources().partition("dev").runtime("4h").memory_per_node("64G").
			nodes(16).node_constraints("fastdata").cpus_per_node(64).total_cpus(64*16);
		j.post_command("md5sum *");
		j.stageout().from("stdout").to("https://external/file");
		System.out.println(j.getJSON().toString(2));
	}

	@Test
	public void testStatusOrder() throws Exception {
		assertTrue(Status.RUNNING.compareTo(Status.READY)>0);
		assertTrue(Status.STAGINGOUT.compareTo(Status.SUCCESSFUL)<0);
	}
}
