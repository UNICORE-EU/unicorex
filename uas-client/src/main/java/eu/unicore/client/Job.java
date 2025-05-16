package eu.unicore.client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.uas.json.JSONUtil;

/**
 * A helper for creating UNICORE jobs
 * 
 * @author schuller
 */
public class Job {

	protected JSONObject json;

	public Job(JSONObject json) {
		this.json = json;
	}

	public Job() {
		this(new JSONObject());
	}

	public final JSONObject getJSON() {
		return json;
	}

	public Job application(String name, String version) {
		if(name==null)throw new IllegalArgumentException("Application name cannot be null");
		JSONUtil.putQuietly(json, "ApplicationName", name);
		if(version!=null)JSONUtil.putQuietly(json, "ApplicationVersion", version);
		return this;
	}

	public Job application(String name) {
		return application(name, null);
	}

	public Job executable(String exe) {
		JSONUtil.putQuietly(json, "Executable", exe);
		return this;
	}

	public Job arguments(Iterable<String> args) {
		JSONArray myargs = JSONUtil.getOrCreateArray(json, "Arguments");
		for(String arg: args) {
			myargs.put(arg);
		}
		return this;
	}

	public Job arguments(String ... args) {
		JSONArray myargs = JSONUtil.getOrCreateArray(json, "Arguments");
		for(String arg: args) {
			myargs.put(arg);
		}
		return this;
	}

	public Job environment(String name, String value) {
		if(name==null || value==null)throw new IllegalArgumentException("Environment variable name/value cannot be null");
		JSONArray env = JSONUtil.getOrCreateArray(json, "Environment");
		env.put(name+"="+value);
		return this;
	}

	public Job parameter(String name, String value) {
		if(name==null || value==null)throw new IllegalArgumentException("Environment variable name/value cannot be null");
		JSONObject env = JSONUtil.getOrCreateObject(json, "Parameters");
		JSONUtil.putQuietly(env, name,value);
		return this;
	}

	public Job pre_command(String cmd) {
		JSONUtil.putQuietly(json, "User precommand", cmd);
		return this;
	}

	public Job post_command(String cmd) {
		JSONUtil.putQuietly(json, "User postcommand", cmd);
		return this;
	}

	public Stage stagein() {
		JSONArray in = JSONUtil.getOrCreateArray(json, "Imports");
		Stage s = new Stage();
		in.put(s.getJSON());
		return s;
	}

	public Stage stageout() {
		JSONArray in = JSONUtil.getOrCreateArray(json, "Exports");
		Stage s = new Stage();
		in.put(s.getJSON());
		return s;
	}

	/**
	 * add required resources
	 */
	public Resources resources() {
		JSONObject res = JSONUtil.getOrCreateObject(json, "Resources");
		return new Resources(res);
	}

	public Job tags(String ... tags) {
		JSONArray mytags = JSONUtil.getOrCreateArray(json, "Tags");
		for(String tag: tags) {
			mytags.put(tag);
		}
		return this;
	}

	public Job name(String name) {
		JSONUtil.putQuietly(json, "Name", name);
		return this;
	}

	/**
	 * Notification URL for receiving callbacks when 
	 * the job enters the 'running' and 'finished' state(s)
	 */
	public Job notify(String url) {
		JSONArray notify = JSONUtil.getOrCreateArray(json, "Notification");
		notify.put(url);
		return this;
	}

	/**
	 * accounting project
	 */
	public Job project(String name) {
		JSONUtil.putQuietly(json, "Project", name);
		return this;
	}

	/**
	 * do not fail the job if user script's exit code is non-zero
	 */
	public Job ignore_exit_code() {
		JSONUtil.putQuietly(json, "IgnoreNonZeroExitCode", "true");
		return this;
	}

	/**
	 * set standard input (default: none)
	 */
	public Job stdin(String input) {
		JSONUtil.putQuietly(json, "Stdin", input);
		return this;
	}

	/**
	 * set standard output (default is 'stdout')
	 */
	public Job stdout(String output) {
		JSONUtil.putQuietly(json, "Stdout", output);
		return this;
	}

	/**
	 * set standard error (default is 'stderr')
	 */
	public Job stderr(String error) {
		JSONUtil.putQuietly(json, "Stderr", error);
		return this;
	}

	public Job run_on_login_node() {
		return run_on_login_node(null);
	}

	public Job run_on_login_node(String nodeSpec) {
		type(Type.ON_LOGIN_NODE);
		if(nodeSpec!=null){
			JSONUtil.putQuietly(json, "Login node", nodeSpec);
		}
		return this;
	}

	public Job wait_for_client_stage_in() {
		JSONUtil.putQuietly(json, "haveClientStageIn", "true");
		return this;
	}

	public Job type(Job.Type type) {
		JSONUtil.putQuietly(json, "Job type", String.valueOf(type));
		return this;
	}

	public static class Stage {

		protected final JSONObject stage;

		public Stage(JSONObject stage) {
			this.stage = stage;
		}

		public Stage() {
			this(new JSONObject());
		}

		public final JSONObject getJSON() {
			return stage;
		}

		public Stage from(String source) throws JSONException {
			stage.put("From", source);
			return this;
		}

		public Stage to(String target) throws JSONException {
			stage.put("To", target);
			return this;
		}

		public Stage data(String data) throws JSONException {
			stage.put("From", "inline://dummy");
			stage.put("Data", data);
			return this;
		}

		public Stage ignore_error() throws JSONException {
			stage.put("FailOnError", "false");
			return this;
		}

		public Credentials with_credentials() throws JSONException {
			Credentials c = new Credentials();
			stage.put("Credentials", c.getJSON());
			return c;
		}
	}

	public static class Credentials {

		protected final JSONObject credentials;

		public Credentials(JSONObject credentials) {
			this.credentials = credentials;
		}

		public Credentials() {
			this(new JSONObject());
		}

		public final JSONObject getJSON() {
			return credentials;
		}

		public void bearerToken(String token) throws JSONException {
			credentials.put("BearerToken", token);
		}

		public void token(String token) throws JSONException {
			credentials.put("Token", token);
		}

		public void username(String username, String password) throws JSONException {
			credentials.put("Username", username);
			credentials.put("Password", password);
		}
	}

	public static class Resources {

		protected final JSONObject resources;

		public Resources(JSONObject resources) {
			this.resources = resources;
		}

		public Resources() {
			this(new JSONObject());
		}

		public final JSONObject getJSON() {
			return resources;
		}

		/**
		 * the partition / batch queue to use
		 */
		public Resources partition(String value) throws JSONException {
			resources.put("Queue", value);
			return this;
		}

		public Resources nodes(int value) throws JSONException {
			resources.put("Nodes", value);
			return this;
		}

		public Resources cpus_per_node(int value) throws JSONException {
			resources.put("CPUsPerNode", value);
			return this;
		}

		public Resources total_cpus(int value) throws JSONException {
			resources.put("CPUs", value);
			return this;
		}

		/**
		 * runtime in seconds (you can use unit suffixes "m", "h", "d")
		 */
		public Resources runtime(String value) throws JSONException {
			resources.put("Runtime", value);
			return this;
		}

		/**
		 * memory per node in bytes (you can use unit suffixes "k", "M", "G")
		 */
		public Resources memory_per_node(String value) throws JSONException {
			resources.put("MemoryPerNode", value);
			return this;
		}

		public Resources node_constraints(String value) throws JSONException {
			resources.put("NodeConstraints", value);
			return this;
		}

		public Resources reservation(String value) throws JSONException {
			resources.put("Reservation", value);
			return this;
		}

		public Resources other(String name, String value) throws JSONException {
			resources.put(name, value);
			return this;
		}
	}

	public enum Type {
		ON_LOGIN_NODE,
		BATCH,
		RAW,
		ALLOCATE,
	}

}
