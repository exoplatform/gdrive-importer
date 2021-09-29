package org.exoplatform.model;

import java.io.Serializable;
import java.util.Date;

public class ClonedGFile implements Serializable {

    private Long id;
    private String gFileId;
    private String gFileNodeId;
    private String fileGDriveLink;
    private String fileExoLink;
    private Date lastModifiedInGDrive;
    private Date lastCloneDate;

    public ClonedGFile(Long id, String gFileId, String gFileNodeId, String fileGDriveLink, String fileExoLink, Date lastModifiedInGDrive, Date lastCloneDate) {
        this.id = id;
        this.gFileId = gFileId;
        this.gFileNodeId = gFileNodeId;
        this.fileGDriveLink = fileGDriveLink;
        this.fileExoLink = fileExoLink;
        this.lastModifiedInGDrive = lastModifiedInGDrive;
        this.lastCloneDate = lastCloneDate;
    }

    public ClonedGFile() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGFileId() {
        return gFileId;
    }

    public void setGFileId(String gFileId) {
        this.gFileId = gFileId;
    }

    public String getGFileNodeId() {
        return gFileNodeId;
    }

    public void setGFileNodeId(String gFileNodeId) {
        this.gFileNodeId = gFileNodeId;
    }

    public String getFileGDriveLink() {
        return fileGDriveLink;
    }

    public void setFileGDriveLink(String fileGDriveLink) {
        this.fileGDriveLink = fileGDriveLink;
    }

    public String getFileExoLink() {
        return fileExoLink;
    }

    public void setFileExoLink(String fileExoLink) {
        this.fileExoLink = fileExoLink;
    }

    public Date getLastModifiedInGDrive() {
        return lastModifiedInGDrive;
    }

    public void setLastModifiedInGDrive(Date lastModifiedInGDrive) {
        this.lastModifiedInGDrive = lastModifiedInGDrive;
    }

    public Date getLastCloneDate() {
        return lastCloneDate;
    }

    public void setLastCloneDate(Date lastCloneDate) {
        this.lastCloneDate = lastCloneDate;
    }
}
