package de.fzj.unicore.xnjs.ems;

public class BudgetInfo {

	private String projectName;
	
	private long remaining;
	
	private String units;
	
	private int percentRemaining;

	public BudgetInfo(String tsiLine){
		String[]parts = tsiLine.split(" +");
		if(parts.length==2){
			parseTSI7(parts);
		}
		else {
			parse(parts);
		}
	}
	
	protected void parse(String[]tokens) {
		this.projectName = tokens[0];
		this.remaining = Long.valueOf(tokens[1]);
		this.percentRemaining = Float.valueOf(tokens[2]).intValue();
		if(percentRemaining<-1 || percentRemaining>100) {
			throw new IllegalArgumentException("Invalid TSI reply: percentage out of range.");
		}
		this.units = tokens[3];
	}
	
	protected void parseTSI7(String[]tokens) {
		this.projectName = CURRENT_PROJECT;
		this.units = "core-h";
		this.remaining = Long.valueOf(tokens[1]);
		this.percentRemaining = -1;
	}
	
	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public long getRemaining() {
		return remaining;
	}

	public void setRemaining(long remaining) {
		this.remaining = remaining;
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public int getPercentRemaining() {
		return percentRemaining;
	}

	public void setPercentRemaining(int percentRemaining) {
		this.percentRemaining = percentRemaining;
	}
	
	public static final String CURRENT_PROJECT = "__CURRENT_PROJECT__";
	
	public String toString() {
		return "BudgetInfo["+projectName+" "+remaining+" "+units+" ("+percentRemaining+"%)]";
	}
}
