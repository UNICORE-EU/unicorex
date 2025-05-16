package eu.unicore.uas.jclouds.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.uas.jclouds.s3.S3StorageAdapterFactory;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.uftp.dpc.Utils;

public class TestS3 {

	@Test
	public void testS3() throws Exception {
		// load creds
		Properties cred = new Properties();
		try(FileInputStream fis = new FileInputStream(
				new File(System.getProperty("user.home"),".aws/unicore-testing.properties"))) 
		{
			cred.load(fis);
		}
		String access = cred.getProperty("accessKey");
		String secret = cred.getProperty("secretKey");
		String endpoint = cred.getProperty("endpoint");
		String bucket = cred.getProperty("bucket");
		String provider = cred.getProperty("provider","aws-s3");
		System.out.println("Accessing endpoint at "+endpoint);
		IStorageAdapter s3 = new S3StorageAdapterFactory().createStorageAdapter(null,access,secret,endpoint,provider,bucket,null,false);
		// create container
		s3.mkdir("/");
		System.out.println(Arrays.asList(s3.ls("testing.unicore.eu")));
		s3.mkdir("/test/mydata");
		// upload some stuff
		String resource = "/test/mydata/in.1";
		OutputStream os = s3.getOutputStream(resource);
		String testdata ="this is some test data";
		String md5 = Utils.md5(testdata.getBytes());
		os.write(testdata.getBytes());
		os.close();
		for(XnjsFile f : s3.ls("/")){
			System.out.println(f+" "+f.getMetadata());
			if(f.getPath().equals(resource)){
				Map<String,String> md = JSONUtil.asMap(new JSONObject(f.getMetadata()));
				assertEquals(md5,md.get("Content-MD5"));
			}
		}
		// download it
		String downloaded = IOUtils.toString(s3.getInputStream(resource), "UTF-8");
		assertEquals(md5,Utils.md5(downloaded.getBytes()));
	}
}
