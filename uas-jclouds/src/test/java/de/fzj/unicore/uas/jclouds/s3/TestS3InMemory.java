package de.fzj.unicore.uas.jclouds.s3;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.jclouds.BlobStoreStorageAdapter;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsFile;
import eu.unicore.uftp.dpc.Utils;
import junit.framework.Assert;

public class TestS3InMemory {


	@Test
	public void testPathHandling() throws Exception {
		BlobStoreStorageAdapter s3 = (BlobStoreStorageAdapter)createS3();
		Pair<String,String>split = s3.splitFullPath("testing.unicore.eu");
		assertEquals("testing.unicore.eu",split.getM1());
		assertTrue(split.getM2().isEmpty());

		split = s3.splitFullPath("/testing.unicore.eu/");
		assertEquals("testing.unicore.eu",split.getM1());
		assertTrue(split.getM2().isEmpty());

		split = s3.splitFullPath("/testing.unicore.eu/directory");
		assertEquals("testing.unicore.eu",split.getM1());
		assertEquals("directory",split.getM2());


		split = s3.splitFullPath("/testing.unicore.eu/directory/subdir/data");
		assertEquals("testing.unicore.eu",split.getM1());
		assertEquals("directory/subdir/data",split.getM2());

		split = s3.splitFullPath("/");
		assertEquals("",split.getM1());
		assertEquals("",split.getM2());
		
		s3.setStorageRoot("testing.unicore.eu");
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
		s3.mkdir("testing.unicore.eu");
		System.out.println(Arrays.asList(s3.ls("testing.unicore.eu/")));
		s3.mkdir("testing.unicore.eu/test");
		assertEquals(0,s3.ls("testing.unicore.eu/test").length);
		s3.mkdir("testing.unicore.eu/test/mydata");
		System.out.println(s3.getProperties("testing.unicore.eu/test/mydata"));
		// upload some stuff
		final String resource = "testing.unicore.eu/test/mydata/in.1";
		OutputStream os = s3.getOutputStream(resource);
		final String testdata ="this is some test data";
		String md5 = Utils.md5(testdata.getBytes());
		os.write(testdata.getBytes());
		os.close();
		for(XnjsFile f : s3.ls("testing.unicore.eu/")){
			System.out.println(f+" "+f.getMetadata());
			if(f.getPath().equals(resource)){
				Map<String,String> md = JSONUtil.asMap(new JSONObject(f.getMetadata()));
				Assert.assertEquals(md5,md.get("Content-MD5"));
			}
		}

		// download it
		String downloaded = IOUtils.toString(s3.getInputStream(resource));
		Assert.assertEquals(md5,Utils.md5(downloaded.getBytes()));

		// delete
		s3.rm(resource);
		assertNull(s3.getProperties(resource));

		// upload in streaming mode
		final String resource2 = "testing.unicore.eu/test/mydata/in.2";

		os = s3.getOutputStream(resource2,false,testdata.length());
		os.write(testdata.getBytes());
		os.close();
		Thread.sleep(1000); // potential race condition
		// download and check
		downloaded = IOUtils.toString(s3.getInputStream(resource2));
		Assert.assertEquals(md5,Utils.md5(downloaded.getBytes()));
	}

	private IStorageAdapter createS3() throws Exception {
		String access = "none";
		String secret = "none";
		String endpoint = "none";
		String provider = "transient";

		return new S3StorageAdapterFactory().createStorageAdapter(null,access,secret,endpoint,provider,null);
	}

}
