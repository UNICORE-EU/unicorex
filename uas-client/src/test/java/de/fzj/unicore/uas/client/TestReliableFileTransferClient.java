package de.fzj.unicore.uas.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import de.fzj.unicore.uas.fts.FiletransferOptions.SupportsPartialRead;
import de.fzj.unicore.uas.fts.rft.LocalStoreImpl;

public class TestReliableFileTransferClient {

	@Test
	public void test1()throws Exception{
		File base=new File("target","rft");
		FileUtils.deleteQuietly(base);
		long totalLength=5*10*1000*1000;
		Source source=new Source(totalLength);
		LocalStoreImpl storage=new LocalStoreImpl(base, "rft-test", totalLength);
		ReliableFileTransferClient rft=new ReliableFileTransferClient(source, storage);
		rft.run();
		System.out.println(rft.getStatusMessage());
		String md5=storage.getDigest();
		System.out.println("Check: original "+source.md5+", got: "+md5);
		assertEquals(source.md5, md5);
	}
	
	static class Source implements SupportsPartialRead{

		static{
			if(Security.getProvider("BC")==null){
				Security.addProvider(new BouncyCastleProvider());
			}
		}
		
		final byte[]data;
		
		public String md5;
		
		public Source(long totalLength){
			data=new byte[(int)totalLength];
			new Random().nextBytes(data);
			md();
		}
		
		private void md(){
			try{
				MessageDigest md=MessageDigest.getInstance(LocalStoreImpl.DIGEST_ALGORITHM,"BC");
				md.update(data);
				md5=hexString(md);
			}catch(NoSuchAlgorithmException m){
				throw new RuntimeException(m);
			}
			catch(NoSuchProviderException m){
				throw new RuntimeException(m);
			}
		}
		
		private static String hexString(MessageDigest digest){
			byte[] messageDigest = digest.digest();
			StringBuilder hexString = new StringBuilder();
			for (int i=0;i<messageDigest.length;i++) {
				String hex = Integer.toHexString(0xFF & messageDigest[i]); 
				if(hex.length()==1)hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		}
		
		@Override
		public long readPartial(long offset, long length, OutputStream os)
				throws IOException {
			byte[]buf=new byte[(int)length];
			System.arraycopy(data, (int)offset, buf, 0, (int)length);
			os.write(buf);
			return length;
		}
	}
}
