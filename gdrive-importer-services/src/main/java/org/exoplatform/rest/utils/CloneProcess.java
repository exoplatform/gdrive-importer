package org.exoplatform.rest.utils;

import org.exoplatform.gdrive.GoogleUser;
import org.exoplatform.listener.CloneGDriveListener;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.security.Identity;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CloneProcess implements CloneGDriveListener {

    /** The process. */
    private final CloneCommand process;

    /** The lock. */
    final Lock lock = new ReentrantLock();

    /** The error. */
    String           error;


    public CloneProcess(GoogleUser user, Node driveNode, String folderOrFileId, String groupId, CloneCommand process, Identity identity) throws RepositoryException {
        this.process = process;
        this.process.start(user, driveNode, folderOrFileId, groupId, identity);
    }

    public String getError() {
        return error;
    }

    public int getProgress() {
        return this.process.getProgress();
    }

    public DriveData getDrive() {
        return this.process.getDrive();
    }

    public CloneCommand getProcess() {
        return process;
    }

    public Lock getLock() {
        return lock;
    }

    public void setError(String error) {
        this.error = error;
    }
}
