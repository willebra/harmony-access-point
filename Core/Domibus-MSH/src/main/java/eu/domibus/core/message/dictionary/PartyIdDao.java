package eu.domibus.core.message.dictionary;

import eu.domibus.api.model.PartyId;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.dao.BasicDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Repository
public class PartyIdDao extends BasicDao<PartyId> {

    @Autowired
    protected DomainContextProvider domainProvider;

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartyIdDao.class);

    public PartyIdDao() {
        super(PartyId.class);
    }

    protected PartyId findPartyByValueAndType(final String value, final String type) {
        final TypedQuery<PartyId> query = this.em.createNamedQuery("PartyId.findByValueAndType", PartyId.class);
        query.setParameter("VALUE", value);
        query.setParameter("TYPE", type);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }

    protected PartyId findByValue(final String value) {
        final TypedQuery<PartyId> query = this.em.createNamedQuery("PartyId.findByValue", PartyId.class);
        query.setParameter("VALUE", value);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }

    public PartyId findExistingPartyId(final String value, String type) {
        if (StringUtils.isNotBlank(type)) {
            return findPartyByValueAndType(value, type);
        }
        return findByValue(value);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PartyId findOrCreateParty(String value, String type) {
        PartyId party = findExistingPartyId(value, type);
        if (party != null) {
            return party;
        }
        PartyId newParty = new PartyId();
        newParty.setValue(value);
        newParty.setType(StringUtils.isNotBlank(type) ? type : null);
        create(newParty);
        return newParty;
    }

    public List<PartyId> searchByValue(final String value) {
        final TypedQuery<PartyId> query = this.em.createNamedQuery("PartyId.searchByValue", PartyId.class);
        query.setParameter("VALUE", value);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return query.getResultList();
    }

}
