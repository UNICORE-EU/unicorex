package de.fzj.unicore.xnjs.io;


public class XnjsStorageInfo {

	private long totalSpace=-1;
	private long freeSpace=-1;
	private long usableSpace=-1;
	
	public long getTotalSpace() {
		return totalSpace;
	}
	public void setTotalSpace(long totalSpace) {
		this.totalSpace = totalSpace;
	}
	public long getFreeSpace() {
		return freeSpace;
	}
	public void setFreeSpace(long freeSpace) {
		this.freeSpace = freeSpace;
	}
	public long getUsableSpace() {
		return usableSpace;
	}
	public void setUsableSpace(long usableSpace) {
		this.usableSpace = usableSpace;
	}
	
	private static int MB=1024*1024;
	
	public String toString(){
		return "Total: "+totalSpace+" ("+totalSpace/MB+"MB) Free: "
		+freeSpace+" ("+freeSpace/MB+"MB) User: "+usableSpace+" ("+usableSpace/MB+"MB)";
	}
	public static XnjsStorageInfo unknown(){
		return new XnjsStorageInfo();
	}
	
}
