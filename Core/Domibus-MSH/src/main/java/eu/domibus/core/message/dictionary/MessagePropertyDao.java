package eu.domibus.core.message.dictionary;

import eu.domibus.api.model.MessageProperty;
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
 * @author François Gautier
 * @since 5.0
 */
@Repository
public class MessagePropertyDao extends BasicDao<MessageProperty> {
    @Autowired
    protected DomainContextProvider domainProvider;

    public MessagePropertyDao() {
        super(MessageProperty.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MessageProperty findOrCreateProperty(final String name, String value, String type) {
        MessageProperty property = findExistingProperty(name, value, type);
        if (property != null) {
            return property;
        }
        MessageProperty newProperty = new MessageProperty();
        newProperty.setName(name);
        newProperty.setValue(value);
        newProperty.setType(StringUtils.isNotBlank(type) ? type : null);
        create(newProperty);
        return newProperty;
    }

    protected MessageProperty findPropertyByNameValueAndType(final String name, String value, String type) {
        final TypedQuery<MessageProperty> query = this.em.createNamedQuery("MessageProperty.findByNameValueAndType", MessageProperty.class);
        query.setParameter("NAME", name);
        query.setParameter("VALUE", value);
        query.setParameter("TYPE", type);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }

    protected MessageProperty findExistingProperty(final String name, String value, String type) {
        if (StringUtils.isNotBlank(type)) {
            return findPropertyByNameValueAndType(name, value, type);
        }
        return findPropertyByNameValue(name, value);
    }

    protected MessageProperty findPropertyByNameValue(final String name, String value) {
        final TypedQuery<MessageProperty> query = this.em.createNamedQuery("MessageProperty.findByNameAndValue", MessageProperty.class);
        query.setParameter("NAME", name);
        query.setParameter("VALUE", value);
        query.setParameter("DOMAIN", domainProvider.getCurrentDomain().getCode());
        return DataAccessUtils.singleResult(query.getResultList());
    }

}
