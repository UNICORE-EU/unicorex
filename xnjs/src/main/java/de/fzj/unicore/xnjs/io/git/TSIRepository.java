package de.fzj.unicore.xnjs.io.git;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsOutputStream;
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription;
import org.eclipse.jgit.internal.storage.dfs.DfsReaderOptions;
import org.eclipse.jgit.internal.storage.dfs.DfsReftableDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.internal.storage.dfs.ReadableChannel;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.internal.storage.reftable.ReftableConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefDatabase;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;

/**
 * Git repository using the TSI. References are stored in-memory,
 * object data is stored in a directory on the TSI.
 * Based on {@link InMemoryRepository}
 *
 * @author schuller
 */
public class TSIRepository extends DfsRepository {

	public static class Builder
			extends DfsRepositoryBuilder<Builder, TSIRepository> {

		private IStorageAdapter tsi;

		private String storageDir;
		
		public Builder setTSI(IStorageAdapter tsi) {
			this.tsi = tsi;
			return this;
		}

		public Builder setStorageDir(String storageDir) {
			this.storageDir = storageDir;
			return this;
		}

		@Override
		public TSIRepository build() throws IOException {
			return new TSIRepository(this, tsi, storageDir);
		}
	}

	static final AtomicInteger packId = new AtomicInteger();

	private final TSIObjDatabase objdb;
	private final MemRefDatabase refdb;
	private String gitwebDescription;

	private static final AtomicLong transferredBytesCounter = new AtomicLong(); 
	
	/**
	 * @param tsi
	 * @param storageDirName - where to place the pack data, relative to TSI storage root
	 */
	public TSIRepository(IStorageAdapter tsi, String storageDirName) {
		this(new Builder().setRepositoryDescription(new DfsRepositoryDescription()), tsi, storageDirName);
	}

	TSIRepository(Builder builder, IStorageAdapter tsi, String storageDirName) {
		super(builder);
		try{
			objdb = new TSIObjDatabase(this, tsi, storageDirName, transferredBytesCounter);
		}catch(ExecutionException ee) {
			throw new RuntimeException("Cannot create directory", ee);
		}
		refdb = createRefDatabase();
	}

	protected MemRefDatabase createRefDatabase() {
		return new MemRefDatabase();
	}

	@Override
	public TSIObjDatabase getObjectDatabase() {
		return objdb;
	}

	@Override
	public RefDatabase getRefDatabase() {
		return refdb;
	}

	@Override
	@Nullable
	public String getGitwebDescription() {
		return gitwebDescription;
	}

	@Override
	public void setGitwebDescription(@Nullable String d) {
		gitwebDescription = d;
	}
	
	public long getTransferredBytes() {
		return transferredBytesCounter.get();
	}

	/** DfsObjDatabase used by TSIRepository. */
	public static class TSIObjDatabase extends DfsObjDatabase {
		private List<DfsPackDescription> packs = new ArrayList<>();
		private Set<ObjectId> shallowCommits = Collections.emptySet();
		private final IStorageAdapter tsi;
		private final String storageDir;	
		private final AtomicLong counter;

		TSIObjDatabase(TSIRepository repo, IStorageAdapter tsi, String storageDir, AtomicLong counter)
				throws ExecutionException {
			super(repo, new DfsReaderOptions());
			this.tsi = tsi;
			this.storageDir = storageDir;
			this.counter = counter;
			createStorageDir();
		}

		private void createStorageDir() throws ExecutionException {
			tsi.mkdir(storageDir);
		}

		@Override
		protected synchronized List<DfsPackDescription> listPacks() {
			return packs;
		}

		@Override
		protected DfsPackDescription newPack(PackSource source) {
			int id = packId.incrementAndGet();
			return new DfsPackDescription(
					getRepository().getDescription(),
					"pack-" + id + "-" + source.name(),
					source);
		}

		@Override
		protected synchronized void commitPackImpl(
				Collection<DfsPackDescription> desc,
				Collection<DfsPackDescription> replace) {
			List<DfsPackDescription> n;
			n = new ArrayList<>(desc.size() + packs.size());
			n.addAll(desc);
			n.addAll(packs);
			if (replace != null)
				n.removeAll(replace);
			packs = n;
			clearCache();
		}

		@Override
		protected void rollbackPack(Collection<DfsPackDescription> desc) {}

		@Override
		protected ReadableChannel openFile(DfsPackDescription desc, PackExt ext)
				throws FileNotFoundException, IOException {
			String fileName = storageDir+"/"+desc.getFileName(ext);
			return new TSIReadableChannel(tsi, fileName);
		}

		@Override
		protected DfsOutputStream writeFile(DfsPackDescription desc,
				PackExt ext) throws IOException {
			String fileName = storageDir+"/"+desc.getFileName(ext);
			return new Out(tsi, fileName, counter);
		}

		@Override
		public Set<ObjectId> getShallowCommits() throws IOException {
			return shallowCommits;
		}

		@Override
		public void setShallowCommits(Set<ObjectId> shallowCommits) {
			this.shallowCommits = shallowCommits;
		}

		@Override
		public long getApproximateObjectCount() {
			long count = 0;
			for (DfsPackDescription p : packs) {
				count += p.getObjectCount();
			}
			return count;
		}
	}

	public static class Out extends DfsOutputStream {
		private final IStorageAdapter tsi;
		private final String fileName;
		private final AtomicLong counter;
		public Out(IStorageAdapter tsi, String fileName, AtomicLong counter) {
			this.tsi = tsi;
			this.fileName = fileName;
			this.counter = counter;
		}

		@Override
		public void write(byte[] buf, int off, int len) throws IOException {
			try(OutputStream dst = tsi.getOutputStream(fileName, true)){
				dst.write(buf, off, len);
				dst.flush();
				counter.addAndGet(len);
			}
		}

		@Override
		public int read(long position, ByteBuffer buf) throws IOException {
			byte[] d = getData();
			int n = Math.min(buf.remaining(), d.length - (int) position);
			if (n == 0)
				return -1;
			buf.put(d, (int) position, n);
			return n;
		}

		byte[] getData() throws IOException {
			try(InputStream is = tsi.getInputStream(fileName)){
				return is.readAllBytes();
			}
		}

	}

	public static class TSIReadableChannel implements ReadableChannel {
		private final IStorageAdapter tsi;
		private final String fileName;
		private int position;
		private long length;
		public TSIReadableChannel(IStorageAdapter tsi, String fileName) throws IOException {
			this.tsi = tsi;
			this.fileName = fileName;
			try{
				this.length = tsi.getProperties(fileName).getSize();
			}catch(ExecutionException ee) {
				throw new IOException(ee);
			}
		}

		@Override
		public int read(ByteBuffer dst) throws IOException {
			long n = Math.min(dst.remaining(), length - position);
			if (n == 0)
				return -1;
			InputStream is = tsi.getInputStream(fileName);
			is.skip(position);
			byte[] data = is.readNBytes((int)n);
			dst.put(data);
			position += n;
			return (int)n;
		}

		@Override
		public void close() {}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public long position() {
			return position;
		}

		@Override
		public void position(long newPosition) {
			position = (int) newPosition;
		}

		@Override
		public long size() {
			return length;
		}

		@Override
		public int blockSize() {
			return 32*1024;
		}

		@Override
		public void setReadAheadBytes(int b) {
			// Unnecessary
		}
	}

	protected class MemRefDatabase extends DfsReftableDatabase {

		protected MemRefDatabase() {
			super(TSIRepository.this);
		}

		@Override
		public ReftableConfig getReftableConfig() {
			ReftableConfig cfg = new ReftableConfig();
			cfg.setAlignBlocks(false);
			cfg.setIndexObjects(false);
			cfg.fromConfig(getRepository().getConfig());
			return cfg;
		}

		@Override
		public boolean performsAtomicTransactions() {
			return true;
		}
	}
}

