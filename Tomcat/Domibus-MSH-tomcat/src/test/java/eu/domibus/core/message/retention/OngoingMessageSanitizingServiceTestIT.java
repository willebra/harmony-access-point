package eu.domibus.core.message.retention;

import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.test.AbstractIT;
import org.apache.commons.lang3.time.DateUtils;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 5.1.4
 */
public class OngoingMessageSanitizingServiceTestIT extends AbstractIT {

    @Autowired
    OngoingMessageSanitizingService ongoingMessageSanitizingService;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @Autowired
    UserMessageLogDao userMessageLogDao;

    @Autowired
    private DateUtil dateUtil;

    @Before
    public void setup() {
        deleteAllMessages();
    }

    @After
    public void clean() {
        deleteAllMessages();
    }

    @Test
    public void findOngoingMessagesWhichAreNotProcessedAnymoreWhenNoOngoingMessagesFound() throws XmlProcessingException, IOException, SQLException {
//        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();

        //persist the change the database
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put("retention_downloaded=\"0\"", "retention_downloaded=\"10\"");
        toReplace.put("retention_undownloaded=\"0\"", "retention_undownloaded=\"10\"");
        uploadPmode(SERVICE_PORT, toReplace);

        UserMessageLog userMessageLog1 = messageDaoTestUtil.createUserMessageLog(
                "msg1@domibus.eu",
                new Date(),
                MSHRole.SENDING,
                MessageStatus.SEND_ENQUEUED,
                false,
                true,
                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC",
                null,
                false);
        UserMessageLog userMessageLog2 = messageDaoTestUtil.createUserMessageLog(
                "msg2@domibus.eu",
                new Date(), MSHRole.SENDING,
                MessageStatus.WAITING_FOR_RETRY,
                false,
                true,
                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC",
                null,
                false);

        final List<EArchiveBatchUserMessage> ongoingMessagesWhichAreNotProcessedAnymore = ongoingMessageSanitizingService.findOngoingMessagesWhichAreNotProcessedAnymore();
        assertEquals(0, ongoingMessagesWhichAreNotProcessedAnymore.size());
    }

    @Test
    public void findOngoingMessagesWhichAreNotProcessedAnymoreWhenOngoingMessagesFound() throws XmlProcessingException, IOException, SQLException {
//        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();

        Map<String, String> toReplace = new HashMap<>();
        toReplace.put("retention_downloaded=\"0\"", "retention_downloaded=\"10\"");
        toReplace.put("retention_undownloaded=\"0\"", "retention_undownloaded=\"10\"");
        uploadPmode(SERVICE_PORT, toReplace);

        //we change the id pk so that the UserMessage and UserMessageLog appear to be received on 24 March 2024 which is a date in the past older than the PMode retention time
        final Date messageReceivedDate = DateUtils.setYears(dateUtil.getUtcDate(), 2024);
        DateUtils.setMonths(messageReceivedDate, 2);
        DateUtils.setDays(messageReceivedDate, 20);

        UserMessageLog userMessageLog1 = messageDaoTestUtil.createUserMessageLog(
                "msg1@domibus.eu",
                messageReceivedDate,
                MSHRole.SENDING,
                MessageStatus.SEND_ENQUEUED,
                false,
                true,
                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC",
                null,
                false);

        //we change the id pk so that the UserMessage and UserMessageLog appear to be received on 24 March 2024 which is a date in the past older than the PMode retention time
        final long newIdPk1 = 240320140000001000L;
        messageDaoTestUtil.updateUserMessageAndUserMessageLogPrimaryKey(userMessageLog1.getEntityId(), newIdPk1);

        UserMessageLog userMessageLog2 = messageDaoTestUtil.createUserMessageLog(
                "msg2@domibus.eu",
                messageReceivedDate,
                MSHRole.SENDING,
                MessageStatus.WAITING_FOR_RETRY,
                false,
                true,
                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC",
                null,
                false);

        //we change the id pk so that the UserMessage and UserMessageLog appear to be received on 24 March 2024 which is a date in the past older than the PMode retention time
        final long newIdPk2 = 240320140000001001L;
        messageDaoTestUtil.updateUserMessageAndUserMessageLogPrimaryKey(userMessageLog2.getEntityId(), newIdPk2);

        final List<EArchiveBatchUserMessage> ongoingMessagesWhichAreNotProcessedAnymore = ongoingMessageSanitizingService.findOngoingMessagesWhichAreNotProcessedAnymore();
        assertEquals(2, ongoingMessagesWhichAreNotProcessedAnymore.size());
    }
}
