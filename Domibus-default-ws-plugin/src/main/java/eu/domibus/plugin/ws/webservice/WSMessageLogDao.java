package eu.domibus.plugin.ws.webservice;

import eu.domibus.plugin.ws.util.WSBasicDao;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author idragusa
 * @since 4.2
 */
@Repository
public class WSMessageLogDao extends WSBasicDao<WSMessageLogEntity> {

    private static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String MESSAGE_IDS = "MESSAGE_IDS";

    private static final String CRIT_MESSAGE_ID = "messageId";
    private static final String CRIT_CONVERSATION_ID = "conversationId";
    private static final String CRIT_REF_TO_MESSAGE_ID = "refToMessageId";
    private static final String CRIT_FROM_PARTY_ID = "fromPartyId";
    private static final String CRIT_FINAL_RECIPIENT= "finalRecipient";
    private static final String CRIT_RECEIVED= "received";
    private static final String CRIT_ORIGINAL_SENDER = "originalSender";


    public WSMessageLogDao() {
        super(WSMessageLogEntity.class);
    }

    /**
     * Find the entry based on a given MessageId.
     *
     * @param messageId the id of the message.
     */
    public WSMessageLogEntity findByMessageId(String messageId) {
        TypedQuery<WSMessageLogEntity> query = em.createNamedQuery("WSMessageLogEntity.findByMessageId", WSMessageLogEntity.class);
        query.setParameter(MESSAGE_ID, messageId);
        WSMessageLogEntity wsMessageLogEntity;
        try {
            wsMessageLogEntity = query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
        return wsMessageLogEntity;
    }

    /**
     * Find all entries in the plugin table limited to maxCount. When maxCount is 0, return all.
     */
    public List<WSMessageLogEntity> findAll(int maxCount) {
        TypedQuery<WSMessageLogEntity> query = em.createNamedQuery("WSMessageLogEntity.findAll", WSMessageLogEntity.class);
        if(maxCount > 0) {
            return query.setMaxResults(maxCount).getResultList();
        }
        return query.getResultList();
    }


    /**
     * Find all entries in the plugin table.
     */
    public List<WSMessageLogEntity> findAll() {
        TypedQuery<WSMessageLogEntity> query = em.createNamedQuery("WSMessageLogEntity.findAll", WSMessageLogEntity.class);
        return query.getResultList();
    }

    /**
     * Find all entries in plugin table based on criteria and limited to maxCount
     * When maxCount is 0, return all.
     *
     * @param messageId
     * @param fromPartyId
     * @param conversationId
     * @param referenceMessageId
     * @param originalSender
     * @param finalRecipient
     * @param receivedFrom
     * @param receivedTo
     * @param maxCount
     * @return List<WSMessageLogEntity>
     */
    public List<WSMessageLogEntity> findAllWithFilter(String messageId, String fromPartyId, String conversationId, String referenceMessageId,
                                                      String originalSender, String finalRecipient, LocalDateTime receivedFrom, LocalDateTime receivedTo,
                                                      int maxCount) {
        TypedQuery<WSMessageLogEntity> query = em.createQuery(
                buildWSMessageLogListCriteria(messageId, conversationId, referenceMessageId, fromPartyId,
                        originalSender, finalRecipient, receivedFrom, receivedTo));

        if (maxCount > 0) {
            query.setMaxResults(maxCount);
        }
        return query.getResultList();
    }


    protected CriteriaQuery<WSMessageLogEntity> buildWSMessageLogListCriteria(String messageId, String conversationId, String refToMessageId, String fromPartyId,
                                                                              String originalSender, String finalRecipient,
                                                                              LocalDateTime receivedFrom, LocalDateTime receivedTo) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<WSMessageLogEntity> criteriaQuery = criteriaBuilder.createQuery(WSMessageLogEntity.class);
        Root<WSMessageLogEntity> root = criteriaQuery.from(WSMessageLogEntity.class);
        criteriaQuery.select(root);
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.isNotBlank(messageId)) {
            predicates.add(criteriaBuilder.equal(root.get(CRIT_MESSAGE_ID), messageId));
        }
        if (StringUtils.isNotBlank(conversationId)) {
            predicates.add(criteriaBuilder.equal(root.get(CRIT_CONVERSATION_ID), conversationId));
        }
        if (StringUtils.isNotBlank(refToMessageId)) {
            predicates.add(criteriaBuilder.equal(root.get(CRIT_REF_TO_MESSAGE_ID), refToMessageId));
        }
        if (StringUtils.isNotBlank(fromPartyId)) {
            predicates.add(criteriaBuilder.equal(root.get(CRIT_FROM_PARTY_ID), fromPartyId));
        }
        if (StringUtils.isNotBlank(finalRecipient)) {
            predicates.add(criteriaBuilder.equal(root.get(CRIT_FINAL_RECIPIENT), finalRecipient));
        }
        if (StringUtils.isNotBlank(originalSender)) {
            predicates.add(criteriaBuilder.equal(root.get(CRIT_ORIGINAL_SENDER), originalSender));
        }
        if (receivedFrom != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Date>get(CRIT_RECEIVED), asDate(receivedFrom)));
        }
        if (receivedTo != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Date>get(CRIT_RECEIVED), asDate(receivedTo)));
        }
        if (CollectionUtils.isNotEmpty(predicates)) {
            criteriaQuery.where(predicates.toArray(new Predicate[]{}));
        }
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get(CRIT_RECEIVED)));
        return criteriaQuery;
    }

    private Date asDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }


    /**
     * Delete the entry related to a given MessageId.
     *
     * @param messageId the id of the message.
     */
    public void deleteByMessageId(final String messageId) {
        Query query = em.createNamedQuery("WSMessageLogEntity.deleteByMessageId");
        query.setParameter(MESSAGE_ID, messageId);
        query.executeUpdate();
    }

    /**
     * Delete the entries related to a given MessageIds.
     *
     * @param messageIds the ids of the messages that should be deleted.
     */
    public void deleteByMessageIds(final List<String> messageIds) {
        Query query = em.createNamedQuery("WSMessageLogEntity.deleteByMessageIds");
        query.setParameter(MESSAGE_IDS, messageIds);
        query.executeUpdate();
    }
}
