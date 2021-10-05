package org.exoplatform.rest.utils;

import org.exoplatform.services.cms.drives.DriveData;

public class ClonedDrive {

    private String name;
    private String workspace;
    private boolean cloned;
    private DriveData driveData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public boolean isCloned() {
        return cloned;
    }

    public void setCloned(boolean cloned) {
        this.cloned = cloned;
    }

    public DriveData getDriveData() {
        return driveData;
    }

    public void setDriveData(DriveData driveData) {
        this.driveData = driveData;
    }
}
