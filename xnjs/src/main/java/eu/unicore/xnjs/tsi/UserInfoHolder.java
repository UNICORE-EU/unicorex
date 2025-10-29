package eu.unicore.xnjs.tsi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserInfoHolder {

	private final List<String> keys = new ArrayList<>();

	private final Map<String,String>attributes = new HashMap<>();

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public List<String> getPublicKeys() {
		return keys;
	}

}
