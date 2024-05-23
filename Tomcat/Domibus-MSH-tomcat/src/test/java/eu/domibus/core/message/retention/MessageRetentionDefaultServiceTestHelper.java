package eu.domibus.core.message.retention;

import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.common.JPAConstants;
import eu.domibus.core.message.PartInfoDao;
import eu.domibus.core.message.UserMessageLogDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Service
public class MessageRetentionDefaultServiceTestHelper {

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private PartInfoDao partInfoDao;

    @Transactional
    public void assertPayloadDeleted(String messageId) {
        UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        em.refresh(userMessageLog);
        assertEquals("Expecting change of status of message", MessageStatus.DELETED, userMessageLog.getMessageStatus());
        long entityId = userMessageLog.getEntityId();
        List<PartInfo> partInfoList = partInfoDao.findPartInfoByUserMessageEntityId(entityId);
        partInfoList.forEach(partInfo -> {
            em.refresh(partInfo);
            assertNull("Expecting payload to be deleted", partInfo.getBinaryData());
        });
    }
}
