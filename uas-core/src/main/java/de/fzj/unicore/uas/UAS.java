package de.fzj.unicore.uas;

// kept for backwards compatibility with existing start scripts
public class UAS extends eu.unicore.uas.UAS {

	public UAS(String configFile) throws Exception {
		super(configFile);
		throw new IllegalStateException("Use 'eu.unicore.uas.UAS' instead.");
	}

	public static void main(String[] args) throws Exception {
		System.err.println("DEPRECATION WARNING: please use main class "
				+ "'eu.unicore.uas.UAS' in start.sh!");
		eu.unicore.uas.UAS.main(args);
	}

}