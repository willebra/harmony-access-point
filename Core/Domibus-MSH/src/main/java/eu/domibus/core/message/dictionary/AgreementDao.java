package eu.domibus.core.message.dictionary;

import eu.domibus.api.model.AgreementRefEntity;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.dao.BasicDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Repository
public class AgreementDao extends BasicDao<AgreementRefEntity> {
    @Autowired
    protected DomainContextProvider domainProvider;

    public AgreementDao() {
        super(AgreementRefEntity.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgreementRefEntity findOrCreateAgreement(String value, String type) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        AgreementRefEntity agreementRef = findExistingAgreement(value, type);
        if (agreementRef != null) {
            return agreementRef;
        }
        AgreementRefEntity newAgreement = new AgreementRefEntity();
        newAgreement.setValue(value);
        newAgreement.setType(StringUtils.isNotBlank(type) ? type : null);
        create(newAgreement);
        return newAgreement;
    }

    protected AgreementRefEntity findExistingAgreement(final String value, String type) {
        if (StringUtils.isNotBlank(type)) {
            return findByValueAndType(value, type);
        }
        return findByValue(value);
    }

    protected AgreementRefEntity findByValueAndType(final String value, final String type) {
        final TypedQuery<AgreementRefEntity> query = this.em.createNamedQuery("AgreementRef.findByValueAndType", AgreementRefEntity.class);
        query.setParameter("VALUE", value);
        query.setParameter("TYPE", type);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }

    protected AgreementRefEntity findByValue(final String value) {
        final TypedQuery<AgreementRefEntity> query = this.em.createNamedQuery("AgreementRef.findByValue", AgreementRefEntity.class);
        query.setParameter("VALUE", value);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }
}
