package eu.unicore.client.data;

import java.util.Properties;

import org.apache.commons.io.IOUtils;

import eu.unicore.client.core.StorageClient;

/**
 * metadata helpers
 * 
 * @author schuller
 */
public class Metadata {

	public static final String CONTENT_TYPE="Content-Type";

	public static final String CONTENT_MD5="Content-MD5";

	public static final String TAGS="Tags";

	public static final String CRAWLER_CONTROL_FILENAME = ".unicore_metadata_control";

	public static void writeCrawlerControlFile(StorageClient sms, String baseDir, CrawlerControl control)throws Exception{
		sms.upload(baseDir+"/"+CRAWLER_CONTROL_FILENAME, -1).
			write(IOUtils.toInputStream(control.toString(), "UTF-8"));
	}

	public static class CrawlerControl {

		private final String[] includes;
		private final String[] excludes;

		private final boolean useDefaultExcludes;

		public CrawlerControl(String[] includes, String[] excludes){
			this(includes,excludes,true);
		}

		public CrawlerControl(String[] includes, String[] excludes, boolean useDefaultExcludes){
			this.includes=includes;
			this.excludes=excludes;
			this.useDefaultExcludes=useDefaultExcludes;
		}

		public boolean isUseDefaultExcludes() {
			return useDefaultExcludes;
		}

		public String[] getIncludes() {
			return includes;
		}

		public String[] getExcludes() {
			return excludes;
		}

		public static CrawlerControl create(Properties p){
			String[]incl=null;
			String[]excl=null;
			String exclS=p.getProperty("exclude");
			if(exclS!=null){
				excl=exclS.split(",");
				for(int i=0;i<excl.length;i++){
					excl[i]=excl[i].trim();
				}
			}
			String inclS=p.getProperty("include");
			if(inclS!=null){
				incl=inclS.split(",");
				for(int i=0;i<incl.length;i++){
					incl[i]=incl[i].trim();
				}
			}
			boolean useDefaultExcludes=Boolean.parseBoolean(p.getProperty("useDefaultExcludes","true"));
			return new CrawlerControl(incl, excl, useDefaultExcludes);
		}

		public String toString(){
			StringBuilder sb=new StringBuilder();
			boolean first=true;
			if(excludes!=null && excludes.length>0){
				sb.append("exclude=");
				for(String e: excludes){
					if(!first){
						sb.append(",");
					}
					else{
						first=false;
					}
					sb.append(e);
				}
				sb.append("\n\n");
			}
			if(includes!=null && includes.length>0){
				sb.append("include=");
				for(String e: includes){
					if(!first){
						sb.append(",");
					}
					else{
						first=false;
					}
					sb.append(e);
				}
				sb.append("\n\n");
			}

			return sb.toString();
		}
	}

}
