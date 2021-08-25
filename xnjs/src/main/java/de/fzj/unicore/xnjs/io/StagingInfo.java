package de.fzj.unicore.xnjs.io;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StagingInfo implements Serializable, Iterable<DataStagingInfo>{

	private static final long serialVersionUID = 1L;

	private List<DataStageInInfo>stageIn = new ArrayList<>();

	private List<DataStageOutInfo>stageOut = new ArrayList<>();

	public StagingInfo(List<? extends DataStagingInfo>toStage){
		for(DataStagingInfo i: toStage){
			if(i instanceof DataStageInInfo){	
				stageIn.add((DataStageInInfo)i);
			}
			else{
				stageOut.add((DataStageOutInfo)i);
			}
		}
	}
	
	@Override
	public Iterator<DataStagingInfo> iterator() {
		List<DataStagingInfo>all = new ArrayList<>();
		all.addAll(stageIn);
		all.addAll(stageOut);
		return all.iterator();
	}

}
