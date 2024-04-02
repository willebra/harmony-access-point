package eu.domibus.core.message;

import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.PartProperty;
import eu.domibus.api.model.PartPropertyRef;
import eu.domibus.core.dao.BasicDao;
import eu.domibus.core.message.dictionary.PartPropertyDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.jpa.QueryHints;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author François Gautier
 * @since 5.0
 */
@Repository
public class PartInfoDao extends BasicDao<PartInfo> {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartInfoDao.class);

    private final PartPropertyDao partPropertyDao;

    public PartInfoDao(PartPropertyDao partPropertyDao) {
        super(PartInfo.class);
        this.partPropertyDao = partPropertyDao;
    }

    public List<PartInfo> findPartInfoByUserMessageEntityId(final Long userMessageEntityId) {
        final Query query = this.em.createNamedQuery("PartInfo.findPartInfos");
        query.setParameter("ENTITY_ID", userMessageEntityId);
        query.setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false);
        List<PartInfo> partInfos = query.getResultList();
        partInfos.forEach(this::setPartProperties)
            setPartProperties(partInfo);
        });
        return partInfos;
    }

    public PartInfo findPartInfoByUserMessageEntityIdAndCid(final Long userMessageEntityId, String cid) {
        final TypedQuery<PartInfo> query = this.em.createNamedQuery("PartInfo.findPartInfoByUserMessageEntityIdAndCid", PartInfo.class);
        query.setParameter("ENTITY_ID", userMessageEntityId);
        query.setParameter("CID", cid);
        query.setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false);
        PartInfo result = DataAccessUtils.singleResult(query.getResultList());
        setPartProperties(result);
        return result;
    }

    public List<String> findFileSystemPayloadFilenames(List<Long> userMessageEntityIds) {
        TypedQuery<String> query = em.createNamedQuery("PartInfo.findFilenames", String.class);
        query.setParameter("MESSAGEIDS", userMessageEntityIds);
        return query.getResultList();
    }

    public void clearDatabasePayloads(final List<PartInfo> partInfos) {
        final Query emptyQuery = em.createNamedQuery("PartInfo.emptyPayloads");
        emptyQuery.setParameter("PARTINFOS", partInfos);
        emptyQuery.executeUpdate();
    }

    public List<Long> findPartInfoLengthByUserMessageEntityId(final Long userMessageEntityId) {
        final Query query = this.em.createNamedQuery("PartInfo.findPartInfosLength");
        query.setParameter("ENTITY_ID", userMessageEntityId);
        query.setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false);
        return query.getResultList();
    }

    private void setPartProperties(PartInfo partInfo) {
        if (partInfo == null) {
            LOG.debug("partInfo is null");
            return;
        }
        if (CollectionUtils.isEmpty(partInfo.getPartPropertyRefs())) {
            LOG.debug("partInfo property refs is null or empty");
            return;
        }
        final List<Long> entityIDs = partInfo.getPartPropertyRefs().stream().map(PartPropertyRef::getPropertyId).collect(Collectors.toList());
        LOG.debug("Getting PartProperty collection for entity IDs: [{}]", entityIDs);
        List<PartProperty> partProperties = partPropertyDao.findByEntityIDs(entityIDs);
        partInfo.setPartProperties(new HashSet<>(partProperties));
    }
}
