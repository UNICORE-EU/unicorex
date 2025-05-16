package eu.unicore.uas.jclouds.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.uas.jclouds.BlobStoreStorageAdapter;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

public class TestS3InMemory {


	@Test
	public void testPathHandling() throws Exception {
		BlobStoreStorageAdapter s3 = (BlobStoreStorageAdapter)createS3();
		s3.setStorageRoot("/");
		s3.mkdir("/");
		// create dirs
		s3.mkdir("foo/bar/ham/spam");
		assertTrue(s3.getProperties("foo").isDirectory());
		assertTrue(s3.getProperties("/foo").isDirectory());
		assertTrue(s3.getProperties("foo/bar/").isDirectory());
		assertTrue(s3.getProperties("foo/bar/ham").isDirectory());
		assertTrue(s3.getProperties("foo/bar/ham/spam").isDirectory());
	}

	@Test
	public void testS3() throws Exception {
		final BlobStoreStorageAdapter s3 = (BlobStoreStorageAdapter)createS3();
		s3.mkdir("/");
		System.out.println(Arrays.asList(s3.ls("testing.unicore.eu/")));
		s3.mkdir("testing.unicore.eu/test");
		assertEquals(0,s3.ls("testing.unicore.eu/test").length);
		s3.mkdir("testing.unicore.eu/test/mydata");
		System.out.println(s3.getProperties("testing.unicore.eu/test/mydata"));
		// upload some stuff
		final String resource = "testing.unicore.eu/test/mydata/in.1";
		final byte[] testdata ="this is some test data".getBytes();
		
		String md5 = Utils.md5(testdata);
		OutputStream os = s3.getOutputStream(resource, false, testdata.length);
		os.write(testdata);
		os.close();
		for(XnjsFile f : s3.ls("testing.unicore.eu/")){
			System.out.println(f+" "+f.getMetadata());
			if(f.getPath().equals(resource)){
				Map<String,String> md = JSONUtil.asMap(new JSONObject(f.getMetadata()));
				assertEquals(md5,md.get("Content-MD5"));
			}
		}

		// download it
		String downloaded = IOUtils.toString(s3.getInputStream(resource), "UTF-8");
		assertEquals(md5,Utils.md5(downloaded.getBytes()));

		// delete
		s3.rm(resource);
		assertNull(s3.getProperties(resource));
	}

	private IStorageAdapter createS3() throws Exception {
		String access = "none";
		String secret = "none";
		String endpoint = "none";
		String provider = "transient";
		String bucket = "test";
		return new S3StorageAdapterFactory().createStorageAdapter(null,access,secret,endpoint,provider,bucket,null,false);
	}

}
