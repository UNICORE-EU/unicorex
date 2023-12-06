package de.fzj.unicore.xnjs.io.git;

import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

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
		this.info = new TransferInfo(UUID.randomUUID().toString(), gitURI.toString(), targetDirectory);
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

	public void run() {
		info.setStatus(Status.RUNNING);
		if(tsi == null){
			tsi = xnjs.getTargetSystemInterface(client);
			if(umask!=null)tsi.setUmask(umask);
		}
		tsi.setStorageRoot(workingDirectory);
		try {
			tsi.mkdir(targetDirectory);
			tsi.setStorageRoot(FilenameUtils.normalize(
					workingDirectory+"/"+targetDirectory, true));
			String storageDirectory = ".unicore_git_repodata";
			TSIRepository repo = new TSIRepository(tsi, storageDirectory);
			try(Git git = new Git(repo)){
				RefSpec refSpec = branch==null?
						new RefSpec("+refs/heads/*:refs/heads/*"):
							new RefSpec("+refs/heads/"+branch+":refs/heads/"+branch);
				FetchResult fr = git.fetch()
						.setRemote(gitURI)
						.setRefSpecs(refSpec)
						.call();
				checkout(git, repo, fr, branch, revisionID);
			}
			info.setTransferredBytes(repo.getTransferredBytes());
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

	private void checkout(Git git, Repository clonedRepo, FetchResult result, String branch, String commitId)
			throws Exception {
		Ref toCheckout = findBranchToCheckout(result, branch);
		RevCommit commit = getCommit(clonedRepo, toCheckout);
		boolean detached = !toCheckout.getName().startsWith(Constants.R_HEADS);
		RefUpdate u = clonedRepo.updateRef(Constants.HEAD, detached);
		u.setNewObjectId(commit.getId());
		u.forceUpdate();
		if(commitId!=null) {
			commit = getCommit(git, clonedRepo, commitId);
		}
		TSIDirCacheCheckout co = new TSIDirCacheCheckout(clonedRepo, commit.getTree(), tsi);
		co.checkout();
		//if (cloneSubmodules)
		//	cloneSubmodules(clonedRepo);
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
			System.out.println("CCC "+rc.getName());
			if(commitId.equals(rc.getName()))
				return rc;
		}
		return commit;
	}
}
