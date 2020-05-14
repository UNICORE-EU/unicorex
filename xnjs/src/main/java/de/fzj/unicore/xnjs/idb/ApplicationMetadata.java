package de.fzj.unicore.xnjs.idb;

import java.util.ArrayList;
import java.util.List;

public class ApplicationMetadata {

	private final List<OptionDescription> options = new ArrayList<>();
	
	public ApplicationMetadata() {}

	public List<OptionDescription> getOptions() {
		return options;
	}
	
	public String toString() {
		return options.toString();
	}

}
