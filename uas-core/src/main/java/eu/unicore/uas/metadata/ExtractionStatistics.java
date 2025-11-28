package eu.unicore.uas.metadata;

import java.io.Serializable;

/**
 * statistics about the metadata extraction process
 * 
 * @author schuller
 */
public class ExtractionStatistics implements Serializable{

	private static final long serialVersionUID = 1L;

	private int documentsProcessed;
	
	private long durationMillis;

	public int getDocumentsProcessed() {
		return documentsProcessed;
	}

	public void setDocumentsProcessed(int documentsProcessed) {
		this.documentsProcessed = documentsProcessed;
	}

	public long getDurationMillis() {
		return durationMillis;
	}

	public void setDurationMillis(long durationMillis) {
		this.durationMillis = durationMillis;
	}
	
}