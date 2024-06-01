package eu.domibus.core.message.dictionary;

import eu.domibus.api.model.PartyRole;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.dao.BasicDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author François Gautier
 * @since 5.0
 */
@Repository
public class PartyRoleDao extends BasicDao<PartyRole> {

    @Autowired
    protected DomainContextProvider domainProvider;

    public PartyRoleDao() {
        super(PartyRole.class);
    }

    public PartyRole findRoleByValue(final String value) {
        final TypedQuery<PartyRole> query = this.em.createNamedQuery("PartyRole.findByValue", PartyRole.class);
        query.setParameter("VALUE", value);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PartyRole findOrCreateRole(String value) {
        if(StringUtils.isBlank(value)) {
            return null;
        }

        PartyRole role = findRoleByValue(value);
        if (role != null) {
            return role;
        }
        PartyRole newRole = new PartyRole();
        newRole.setValue(value);
        create(newRole);
        return newRole;
    }
}
