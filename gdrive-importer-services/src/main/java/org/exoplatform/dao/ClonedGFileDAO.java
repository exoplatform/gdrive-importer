package org.exoplatform.dao;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.entity.ClonedGFileEntity;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class ClonedGFileDAO extends GenericDAOJPAImpl<ClonedGFileEntity, Long> {

    @Override
    public Long count() {
        return super.count();
    }

    @Override
    public ClonedGFileEntity find(Long aLong) {
        return super.find(aLong);
    }

    @Override
    @ExoTransactional
    public ClonedGFileEntity create(ClonedGFileEntity entity) {
        return super.create(entity);
    }

    public String getExoLinkByGFileId(String id) {
        TypedQuery<String> query = getEntityManager().createNamedQuery("getExoLinkByGFileId", String.class);
        query.setParameter("id", id);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public String getGFileLinkByNodeUUID(String uuid) {
        TypedQuery<String> query = getEntityManager().createNamedQuery("getGDriveLinkByNodeUUID", String.class);
        query.setParameter("uuid", uuid);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public ClonedGFileEntity getClonedFileByGLink(String id) {
        TypedQuery<ClonedGFileEntity> query = getEntityManager().createNamedQuery("getClonedFileByGLink", ClonedGFileEntity.class);
        query.setParameter("id", id);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @ExoTransactional
    public ClonedGFileEntity update(ClonedGFileEntity entity) {
        return super.update(entity);
    }

    @Override
    public void delete(ClonedGFileEntity entity) {
        super.delete(entity);
    }

    @Override
    public List<ClonedGFileEntity> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createAll(List<ClonedGFileEntity> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAll(List<ClonedGFileEntity> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(List<ClonedGFileEntity> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }
}
