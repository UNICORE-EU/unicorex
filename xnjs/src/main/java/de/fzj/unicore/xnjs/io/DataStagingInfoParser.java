package de.fzj.unicore.xnjs.io;

import java.util.List;


/**
 * parses {@link DataStageInInfo} from a source format (XML, JSON, ...)
 * 
 * @author schuller
 *
 * @param <T> - the source object type
 */
public interface DataStagingInfoParser<T> {

	public List<DataStageInInfo> parseImports(T sourceInfo) throws Exception;
	
	public List<DataStageOutInfo> parseExports(T sourceInfo) throws Exception;
	
}
