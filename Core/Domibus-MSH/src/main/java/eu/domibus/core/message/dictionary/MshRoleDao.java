package eu.domibus.core.message.dictionary;

import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MSHRoleEntity;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.dao.SingleValueDictionaryDao;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;

/**
 * @author Cosmin Baciu
 * @implNote This DAO class works with {@link MSHRoleEntity}, which is a static dictionary
 * based on the {@link MSHRole} enum: no new values are expected to be added at runtime;
 * therefore, {@code MshRoleDao} can be used directly, without subclassing {@link AbstractDictionaryService}.
 * @since 5.0
 */
@Repository
public class MshRoleDao extends SingleValueDictionaryDao<MSHRoleEntity> {

    protected final DomainContextProvider domainProvider;

    public MshRoleDao(DomainContextProvider domainProvider) {
        super(MSHRoleEntity.class);
        this.domainProvider = domainProvider;
    }

    @Transactional
    public MSHRoleEntity findOrCreate(final MSHRole role) {
        if (role == null) {
            return null;
        }

        MSHRoleEntity mshRoleEntity = findByRole(role);
        if (mshRoleEntity != null) {
            return mshRoleEntity;
        }
        MSHRoleEntity entity = new MSHRoleEntity();
        entity.setRole(role);
        create(entity);
        return entity;
    }

    public MSHRoleEntity findByRole(final MSHRole role) {
        return getEntity(role);
    }

    @Override
    public MSHRoleEntity findByValue(final Object role) {
        return getEntity(role);
    }

    private MSHRoleEntity getEntity(Object role) {
        final TypedQuery<MSHRoleEntity> query = this.em.createNamedQuery("MSHRoleEntity.findByValue", MSHRoleEntity.class);
        query.setParameter("ROLE", role);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }
}
