package de.fzj.unicore.uas.fts.rft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.fzj.unicore.uas.client.ReliableFileTransferClient.Chunk;
import de.fzj.unicore.uas.client.ReliableFileTransferClient.Store;

/**
 * {@link Store} implementation using {@link File}
 */
public class LocalStoreImpl extends AbstractStoreImpl {

	private final File baseDir;

	private MessageDigest messageDigest;
	
	private byte[]digest;
	
	public static final String DIGEST_ALGORITHM="SHA1";
	
	static{
		if(Security.getProvider("BC")==null){
			Security.addProvider(new BouncyCastleProvider());
		}
	}
	
	public LocalStoreImpl(File baseDir, String target, long totalLength){
		super(target,totalLength);
		this.baseDir=baseDir;
	}
	
	/**
	 * merges the chunks, while calculating a digest
	 */
	@Override
	public void finish() throws Exception {
		File file=new File(baseDir, target);
		try{
			messageDigest=MessageDigest.getInstance(DIGEST_ALGORITHM,"BC");
		}catch(NoSuchAlgorithmException n){
			throw new RuntimeException(n);
		}
		OutputStream os=new DigestOutputStream(new FileOutputStream(file), messageDigest);
		try{
			for(String s: getFileNameList()){
				InputStream is=new FileInputStream(new File(baseDir,new File(partsDir, s).getPath()));
				IOUtils.copy(is, os);
				os.flush();
				is.close();
			}
		}
		finally{
			os.close();
			digest=messageDigest.digest();
		}
		if(file.length()!=totalLength)throw new IOException("Final size does not match expected size");
	}
	
	public String getDigest(){
		StringBuilder hexString = new StringBuilder();
		for (int i=0;i<digest.length;i++) {
			String hex = Integer.toHexString(0xFF & digest[i]); 
			if(hex.length()==1)hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
	

	@Override
	protected boolean checkOK(Chunk chunk) throws Exception {
		File f=new File(baseDir,chunk.getPath());
		return f.exists() && f.length()==chunk.getLength();
	}

	@Override
	protected long getActualFileSize(Chunk chunk) throws Exception {
		File f=new File(baseDir,chunk.getPath());
		return f.exists()? f.length() : -1;
	}

	@Override
	protected Chunk createChunk(int index, String path, long offset, long length) {
		return new LocalChunkImpl(baseDir, index, path, offset, length);
	}

	@Override
	protected void createPartsDirIfNotExists() throws Exception {
		File parts=new File(baseDir,partsDir);
		if(!parts.exists()){
			parts.mkdirs();
		}
	}

	@Override
	protected void loadPropertiesFileIfExists() throws Exception {
		File props=new File(baseDir,propertiesPath);
		if(props.exists()){
			InputStream is=new FileInputStream(props);
			try{
				properties.load(is);
			}
			finally{
				is.close();
			}
		}
	}

	@Override
	protected OutputStream getOutputStream(String path, boolean append)
			throws IOException {
		return new FileOutputStream(new File(baseDir,path), append);
	}
	
}
