package eu.unicore.xnjs.io.git;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.DataStagingCredentials;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.impl.UsernamePassword;
/**
 * stage in a repo from a Git URL
 *
 * @author schuller
 */
public class GitStageIn implements IFileTransfer {

	private final String workingDirectory;
	private final Client client;
	private final XNJS xnjs;
	private final DataStagingCredentials credentials;
	private String preferredLoginNode = null;
	private final String gitURI;
	private final String targetDirectory;
	private String umask = null;
	private final TransferInfo info;
	private String branch = null;
	private String revisionID = null;

	public GitStageIn(XNJS xnjs, Client client, String workingDirectory,
			String gitURI, String targetDirectory,	DataStagingCredentials credentials ) {
		this.xnjs = xnjs;
		this.client = client;
		this.credentials = credentials;
		this.workingDirectory = workingDirectory;
		this.gitURI = gitURI;
		this.targetDirectory = targetDirectory;
		this.info = new TransferInfo(UUID.newUniqueID(), gitURI.toString(), targetDirectory);
		info.setProtocol("git");
	}

	@Override
	public TransferInfo getInfo(){
		return info;
	}

	@Override
	public void setUmask(String umask) {
		this.umask = umask;
	}

	@Override
	public void setPreferredLoginNode(String loginNode) {
		this.preferredLoginNode = loginNode;
	}

	public void run() {
		info.setStatus(Status.RUNNING);
		if(tsi == null){
			tsi = xnjs.getTargetSystemInterface(client, preferredLoginNode);
			if(umask!=null)tsi.setUmask(umask);
		}
		tsi.setStorageRoot(workingDirectory);
		try {
			tsi.mkdir(targetDirectory);
			tsi.setStorageRoot(FilenameUtils.normalize(
					workingDirectory+"/"+targetDirectory, true));
			long transferredBytes = clone(tsi, gitURI, branch, revisionID);
			info.setTransferredBytes(transferredBytes);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED, Log.createFaultMessage("Writing to '"
					+ targetDirectory + "' failed", ex));
		}
	}

	@Override
	public void abort(){}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		//NOP
	}

	@Override
	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}

	private IStorageAdapter tsi;

	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.tsi = adapter;
	}

	@Override
	public void setExtraParameters(Map<String,String> options) {
		if(options!=null) {
			branch = options.get("branch");
			revisionID = options.get("commit");
		}
	}

	private long clone(IStorageAdapter tsi, String url, String branch, String revisionID) throws Exception {
		try(TSIRepository repo = new TSIRepository(tsi, ".unicore_git_repodata")){
			Git git = new Git(repo);
			RefSpec refSpec = branch==null?
					new RefSpec("+refs/heads/*:refs/heads/*") :
						new RefSpec("+refs/heads/"+branch+":refs/heads/"+branch);
			FetchResult fr = git.fetch()
					.setCredentialsProvider(makeGitCredentials())
					.setRemote(url)
					.setRefSpecs(refSpec)
					.call();
			checkout(git, repo, url, fr, branch, revisionID);
			return repo.getTransferredBytes();
		}
	}
	
	private CredentialsProvider makeGitCredentials() {
		if(credentials instanceof UsernamePassword) {
			UsernamePassword up = (UsernamePassword)credentials;
			return new UsernamePasswordCredentialsProvider(up.getUser(),
					up.getPassword().toCharArray());
		}
		return null;
	}
	
	private void checkout(Git git, TSIRepository clonedRepo, String remoteURL, FetchResult result, String branch, String commitId)
			throws Exception {
		Ref toCheckout = findBranchToCheckout(result, branch);
		RevCommit commit = getCommit(clonedRepo, toCheckout);
		boolean detached = !toCheckout.getName().startsWith(Constants.R_HEADS);
		RefUpdate u = clonedRepo.updateRef(Constants.HEAD, detached);
		u.setNewObjectId(commit.getId());
		u.forceUpdate();
		if(commitId!=null) {
			commit = getCommit(git, clonedRepo, commitId);
			if(commit==null) {
				throw new Exception("Requested commit <"+commitId+"> not found");
			}
		}
		u = clonedRepo.updateRef(Constants.HEAD, detached);
		u.setNewObjectId(commit.getId());
		u.forceUpdate();
		TSIDirCacheCheckout co = new TSIDirCacheCheckout(clonedRepo, commit.getTree(), tsi);
		co.checkout();
		if (tsi.getProperties(".gitmodules")!=null) {
			cloneSubmodules(clonedRepo, co, remoteURL);
		}
	}

	private Ref findBranchToCheckout(FetchResult result, String branch) {
		if(branch == null) {
			branch = Constants.HEAD;
		}else {
			branch = "/" + branch;
		}
		Ref idHEAD = null;
		for(Ref r: result.getAdvertisedRefs()){
			if(r.getName().endsWith(branch)) {
				idHEAD = r;
				break;
			}
		}
		ObjectId headId = idHEAD != null ? idHEAD.getObjectId() : null;
		if (headId == null) {
			return null;
		}

		if (idHEAD != null && idHEAD.isSymbolic()) {
			return idHEAD.getTarget();
		}

		Ref master = result.getAdvertisedRef(Constants.R_HEADS
				+ Constants.MASTER);
		ObjectId objectId = master != null ? master.getObjectId() : null;
		if (headId.equals(objectId)) {
			return master;
		}

		Ref foundBranch = null;
		for (Ref r : result.getAdvertisedRefs()) {
			if (headId.equals(r.getObjectId())) {
				foundBranch = r;
				break;
			}
		}
		return foundBranch;
	}

	private RevCommit getCommit(Repository clonedRepo, Ref ref)
			throws Exception {
		final RevCommit commit;
		try (RevWalk rw = new RevWalk(clonedRepo)) {
			commit = rw.parseCommit(ref.getObjectId());
		}
		return commit;
	}

	private RevCommit getCommit(Git git, Repository clonedRepo, String commitId)
			throws Exception {
		RevCommit commit = null;
		for(RevCommit rc: git.log().call()) {
			if(rc.getName().startsWith(commitId))
				return rc;
		}
		return commit;
	}

	private AtomicInteger recursionDepth = new AtomicInteger(0);

	private void cloneSubmodules(TSIRepository repo, TSIDirCacheCheckout co, String parentURL) throws Exception {
		if(recursionDepth.incrementAndGet()>10) {
			throw new Exception("Submodules are too deeply nested for my taste. "
					+ "Rethink your life choices.");
		}
		Map<String, String> submodules = repo.getSubmodules();
		for(String path: submodules.keySet()) {
			String url = getSubmoduleURL(submodules.get(path), parentURL);
			String revision = co.getSubmoduleRevision(path);
			String oldWorkDir = tsi.getStorageRoot();
			String newWorkDir = FilenameUtils.normalize(
					oldWorkDir+"/"+path, true);
			tsi.setStorageRoot(newWorkDir);
			clone(tsi, url, null, revision);
			tsi.setStorageRoot(oldWorkDir);
		}
	}
	
	private String getSubmoduleURL(String url, String parentUrl) {
		if (!url.startsWith("./") && !url.startsWith("../"))
			return url;

		if (parentUrl.charAt(parentUrl.length() - 1) == '/')
			parentUrl = parentUrl.substring(0, parentUrl.length() - 1);
		char separator = '/';
		String submoduleUrl = url;
		while (submoduleUrl.length() > 0) {
			if (submoduleUrl.startsWith("./"))
				submoduleUrl = submoduleUrl.substring(2);
			else if (submoduleUrl.startsWith("../")) {
				int lastSeparator = parentUrl.lastIndexOf('/');
				if (lastSeparator < 1) {
					lastSeparator = parentUrl.lastIndexOf(':');
					separator = ':';
				}
				if (lastSeparator < 1)
					throw new IllegalArgumentException("Illegal URL");
				parentUrl = parentUrl.substring(0, lastSeparator);
				submoduleUrl = submoduleUrl.substring(3);
			} else
				break;
		}
		return parentUrl + separator + submoduleUrl;
	}
	
}