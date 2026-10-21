package eu.unicore.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.client.core.JobClient.Status;

public class TestJob {

	@Test
	public void testJobBuilder() throws Exception {
		Job j = new Job();
		j.tags("test", "foo");
		j.type(Job.Type.BATCH);
		j.project("proj1");
		j.pre_command("unzip data.zip");
		j.application("Date", "1.0");
		j.arguments("-rfc","-v");
		j.environment("FOO", "bar").parameter("SOURCE", "./input.sh");
		j.stdout("stdout");
		j.stderr("stderr");
		j.stdin("/dev/random");
		j.stagein().from("http://google.de").to("index.html").ignore_error();
		j.stagein().from("https://myserver.com/1").to("data1.txt")
			.ignore_error()
			.with_credentials().bearerToken("123");
		j.stagein().from("https://myserver.com/2").to("data2.txt").with_credentials().username("foo", "123");
		j.stagein().data("this is some test data").to("data.zip");
		j.resources().partition("dev").runtime("4h").memory_per_node("64G").
			nodes(16).node_constraints("fastdata").cpus_per_node(64).total_cpus(64*16).
			reservation("test123").other("Network", "fast");
		j.post_command("md5sum *");
		j.stageout().from("stdout").to("https://external/file");
		assertNotNull(j.resources().getJSON().get("Network"));
		System.out.println(j.getJSON().toString(2));
	}

	@Test
	public void testJobBuilder2() throws Exception {
		Job j = new Job();
		j.type(Job.Type.ON_LOGIN_NODE);
		j.application("Checksum");
		j.wait_for_client_stage_in();
		j.run_on_login_node("login1*");
		j.name("test123");
		j.ignore_exit_code();
		j.notify("http://msgserver");
		System.out.println(j.getJSON().toString(2));
	}

	@Test
	public void testStatusOrder() throws Exception {
		assertTrue(Status.RUNNING.compareTo(Status.READY)>0);
		assertTrue(Status.STAGINGOUT.compareTo(Status.SUCCESSFUL)<0);
	}
}
