package org.exoplatform.utils;

import org.exoplatform.entity.ClonedGFileEntity;
import org.exoplatform.model.ClonedGFile;

public class EntityBuilder {
    public static ClonedGFileEntity toEntity(ClonedGFile clonedGFile) {
        ClonedGFileEntity clonedGFileEntity = new ClonedGFileEntity();
        clonedGFileEntity.setId(clonedGFile.getId());
        clonedGFileEntity.setGFileId(clonedGFile.getGFileId());
        clonedGFileEntity.setGFileNodeId(clonedGFile.getGFileNodeId());
        clonedGFileEntity.setFileGDriveLink(clonedGFile.getFileGDriveLink());
        clonedGFileEntity.setFileExoLink(clonedGFile.getFileExoLink());
        clonedGFileEntity.setLastModifiedInGDrive(clonedGFile.getLastModifiedInGDrive());
        clonedGFileEntity.setLastCloneDate(clonedGFile.getLastCloneDate());

        return  clonedGFileEntity;
    }

    public static ClonedGFile fromEntity(ClonedGFileEntity clonedGFileEntity) {
        ClonedGFile clonedGFile = new ClonedGFile();
        clonedGFile.setId(clonedGFileEntity.getId());
        clonedGFile.setGFileId(clonedGFileEntity.getGFileId());
        clonedGFile.setGFileNodeId(clonedGFileEntity.getGFileNodeId());
        clonedGFile.setFileGDriveLink(clonedGFileEntity.getFileGDriveLink());
        clonedGFile.setFileExoLink(clonedGFileEntity.getFileExoLink());
        clonedGFile.setLastModifiedInGDrive(clonedGFileEntity.getLastModifiedInGDrive());
        clonedGFile.setLastCloneDate(clonedGFileEntity.getLastCloneDate());

        return clonedGFile;
    }
}
