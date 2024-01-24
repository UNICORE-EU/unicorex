package eu.unicore.xnjs.io;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.persist.util.UUID;

@Ignore
public class TestingIOCapabilities implements IOCapabilities {

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends IFileTransferCreator>[] getFileTransferCreators() {
		return new Class[]{TestingTransferCreator.class};
	}

	public static class TestingTransferCreator implements IFileTransferCreator{
		XNJS config;
		public TestingTransferCreator(XNJS config){
			this.config = config;
		}
		public IFileTransfer createFileImport(Client client,
				String workingDirectory, DataStageInInfo info) {
			URI source = info.getSources()[0];
			String scheme=source.getScheme();
			if("test".equals(scheme)){
				return new MockImport(config, workingDirectory, source, info.getFileName());
			}
			return null;
		}

		@Override
		public IFileTransfer createFileExport(Client client,String workingDirectory,DataStageOutInfo info) {
			return null;
		}

		@Override
		public String getProtocol() {
			return "test";
		}

		@Override
		public String getStageInProtocol() {
			return getProtocol();
		}

		@Override
		public String getStageOutProtocol() {
			return getProtocol();
		}

	}

	//for testing, this will just "import" some random data into
	//the target file
	public static class MockImport implements IFileTransfer{

		private final String wd;
		private IStorageAdapter storage;
		private TransferInfo info;

		public MockImport(XNJS config, String wd, URI source, String target){
			this.wd=wd;
			this.info = new TransferInfo(UUID.newUniqueID(), source.getRawPath(), target);
			this.info.setProtocol("test");
		}


		@Override
		public void run() {
			try{
				byte[] data=("this is a test import into "+wd).getBytes();
				info.setStatus(Status.RUNNING);
				if(storage==null){
					File file=new File(wd,info.getTarget());
					FileUtils.writeByteArrayToFile(file, data);
				}
				else{
					storage.setStorageRoot(wd);
					try(OutputStream os = storage.getOutputStream(info.getTarget())){
						IOUtils.write(data, os);
					}
				}
				info.setStatus(Status.DONE);
			}
			catch(Exception ex){
				ex.printStackTrace();
				info.setStatus(Status.FAILED);
			}
		}

		@Override
		public void abort(){}

		@Override
		public TransferInfo getInfo() {
			return info;
		}

		@Override
		public void setOverwritePolicy(OverwritePolicy overwrite) {
		}

		@Override
		public void setImportPolicy(ImportPolicy overwrite) {
		}

		@Override
		public void setStorageAdapter(IStorageAdapter adapter) {
			this.storage=adapter;
		}

	}

}
