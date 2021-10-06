package org.exoplatform.storage;

import org.exoplatform.dao.ClonedGFileDAO;
import org.exoplatform.entity.ClonedGFileEntity;
import org.exoplatform.model.ClonedGFile;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.utils.EntityBuilder;

public class ClonedGFileStorage {

    protected static final Log LOG                    = ExoLogger.getLogger(ClonedGFileStorage.class);

    private ClonedGFileDAO clonedGFileDAO;
    private ListenerService listenerService;

    public ClonedGFileStorage(ClonedGFileDAO clonedGFileDAO, ListenerService listenerService) {
        this.clonedGFileDAO = clonedGFileDAO;
        this.listenerService = listenerService;
    }

    public ClonedGFile addClonedFile(ClonedGFile clonedGFile) throws Exception {
        ClonedGFileEntity clonedGFileEntity = EntityBuilder.toEntity(clonedGFile);
        clonedGFileEntity = clonedGFileDAO.create(clonedGFileEntity);
        ClonedGFile clonedFile = EntityBuilder.fromEntity(clonedGFileEntity);
        listenerService.broadcast("exo.copy-gdrive-extension-file-cloned", clonedFile, null);
        return clonedFile;
    }

    public String getExoLinkByGFileId(String id) {
        return clonedGFileDAO.getExoLinkByGFileId(id);
    }

    public String getExoLinkByCSNRef(String ref) {
        return clonedGFileDAO.getExoLinkByCSNRef(ref);
    }

    public String getGFileLinkByNodeUUID(String uuid) {
        return clonedGFileDAO.getExoLinkByGFileId(uuid);
    }

    public ClonedGFile getClonedFileByGLink (String id) {
        ClonedGFileEntity entity = clonedGFileDAO.getClonedFileByGLink(id);
        if(entity != null) {
            return EntityBuilder.fromEntity(clonedGFileDAO.getClonedFileByGLink(id));
        }
        return null;
    }
    public ClonedGFile update(ClonedGFile clonedGFile) {
        ClonedGFileEntity entity = clonedGFileDAO.update(EntityBuilder.toEntity(clonedGFile));
        return EntityBuilder.fromEntity(entity);
    }
}
