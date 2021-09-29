package org.exoplatform.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "ClonedGFileEntity")
@ExoEntity
@Table(name = "COPY_GDRIVE_CL_GFILE")
@NamedQueries({
        @NamedQuery(
                name = "getExoLinkByGFileId",
                query = "SELECT gfile.fileExoLink FROM ClonedGFileEntity gfile WHERE gfile.gFileId =:id"
        ),
        @NamedQuery(
                name = "getGDriveLinkByNodeUUID",
                query = "SELECT gfile.fileGDriveLink FROM ClonedGFileEntity gfile WHERE gfile.gFileNodeId =:uuid"
        ),
        @NamedQuery(
                name= "getClonedFileByGLink",
                query = "SELECT gfile FROM ClonedGFileEntity gfile WHERE gfile.gFileId =:id"
        )
})
public class ClonedGFileEntity implements Serializable {
    @Id
    @SequenceGenerator(name = "SEQ_COPY_GDRIVE_Cl_GFILE_ID", sequenceName = "SEQ_COPY_GDRIVE_Cl_GFILE_ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_COPY_GDRIVE_Cl_GFILE_ID")
    @Column(name = "CL_ID")
    private Long              id;

    @Column(name = "CL_GFILE_ID", nullable = false)
    private String gFileId;

    @Column(name = "CL_GFILE_NODE_UUID", nullable = false)
    private String gFileNodeId;

    @Column(name = "CL_GFILE_LINK_GDRIVE", nullable = false)
    private String fileGDriveLink;

    @Column(name = "CL_GFILE_LINK_EXO", nullable = false)
    private String fileExoLink;

    @Column(name = "CL_GFILE_LAST_MODIFIED", nullable = false)
    private Date lastModifiedInGDrive;

    @Column(name = "CL_GFILE_LAST_CLONE_DATE", nullable = false)
    private Date lastCloneDate;

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
