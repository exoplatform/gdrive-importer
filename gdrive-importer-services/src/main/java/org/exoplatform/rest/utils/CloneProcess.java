package org.exoplatform.rest.utils;

import org.exoplatform.listener.CloneGDriveListener;
import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.drives.DriveData;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CloneProcess implements CloneGDriveListener {

    /** The drive. */
    private final DriveData drive;

    /** The process. */
    private final CloneCommand process;

    private int progress;

    /** The workspace name. */
    final String     workspaceName;

    /** The lock. */
    final Lock lock = new ReentrantLock();

    /** The error. */
    String           error;

    public CloneProcess(DriveData drive, CloneCommand process, String workspaceName) throws CloudDriveException {
        this.drive = drive;
        this.process = process;
        this.workspaceName = workspaceName;
    }

    public String getError() {
        return error;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public DriveData getDrive() {
        return drive;
    }

    public CloneCommand getProcess() {
        return process;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public Lock getLock() {
        return lock;
    }

    public void setError(String error) {
        this.error = error;
    }
}
