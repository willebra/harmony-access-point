package eu.domibus.core.message.retention;

import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.test.AbstractIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.domibus.api.model.DomibusDatePrefixedSequenceIdGeneratorGenerator.MIN;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ONGOING_MESSAGES_SANITIZING_WORKER_DELAY_HOURS;
import static org.junit.Assert.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 5.1.4
 */
public class OngoingMessageSanitizingServiceTestIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(OngoingMessageSanitizingServiceTestIT.class);

    @Autowired
    OngoingMessageSanitizingService ongoingMessageSanitizingService;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @Autowired
    UserMessageLogDao userMessageLogDao;

    @Autowired
    private DateUtil dateUtil;

    @Autowired
    private DomibusPropertyProvider domibusPropertyProvider;

    private long counter = 1;

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
        //persist the change the database
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put("retention_downloaded=\"0\"", "retention_downloaded=\"10\"");
        toReplace.put("retention_undownloaded=\"0\"", "retention_undownloaded=\"10\"");
        uploadPmode(SERVICE_PORT, toReplace);
        UserMessageLog userMessageLog1 = messageDaoTestUtil.createUserMessageLog(
                "msg1@domibus.eu",
                new Date(),
                MessageStatus.SEND_ENQUEUED);
        UserMessageLog userMessageLog2 = messageDaoTestUtil.createUserMessageLog(
                "msg2@domibus.eu",
                new Date(),
                MessageStatus.WAITING_FOR_RETRY);

        final List<EArchiveBatchUserMessage> ongoingMessagesWhichAreNotProcessedAnymore = ongoingMessageSanitizingService.findOngoingMessagesWhichAreNotProcessedAnymore();
        assertEquals(0, ongoingMessagesWhichAreNotProcessedAnymore.size());
    }

    @Test
    public void findOngoingMessagesWhichAreNotProcessedAnymoreWhenOngoingMessagesFound_extendedDelay() throws XmlProcessingException, IOException, SQLException {
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put("retention_downloaded=\"0\"", "retention_downloaded=\"10\"");
        toReplace.put("retention_undownloaded=\"0\"", "retention_undownloaded=\"10\"");
        uploadPmode(SERVICE_PORT, toReplace);

        domibusPropertyProvider.setProperty(DOMIBUS_ONGOING_MESSAGES_SANITIZING_WORKER_DELAY_HOURS, "3");

        createMessage("msg1_found@domibus.eu",  Date.from(LocalDate.of(2024, 2, 20).atStartOfDay().toInstant(ZoneOffset.UTC)), MessageStatus.SEND_ENQUEUED);
        createMessage("msg2_found@domibus.eu",  Date.from(LocalDate.of(2024, 2, 20).atStartOfDay().toInstant(ZoneOffset.UTC)), MessageStatus.WAITING_FOR_RETRY);

        createMessage("msg1_Notfound@domibus.eu",  Date.from(LocalDateTime.now().minusMinutes(5).toInstant(ZoneOffset.UTC)), MessageStatus.SEND_ENQUEUED);
        createMessage("msg2_Notfound@domibus.eu",  Date.from(LocalDateTime.now().minusMinutes(120).toInstant(ZoneOffset.UTC)), MessageStatus.SEND_ENQUEUED);

        final List<EArchiveBatchUserMessage> ongoingMessagesWhichAreNotProcessedAnymore = ongoingMessageSanitizingService.findOngoingMessagesWhichAreNotProcessedAnymore();
        assertEquals(2, ongoingMessagesWhichAreNotProcessedAnymore.size());
    }

    @Test
    public void findOngoingMessagesWhichAreNotProcessedAnymoreWhenOngoingMessagesFound_reducedDelay() throws XmlProcessingException, IOException, SQLException {
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put("retention_downloaded=\"0\"", "retention_downloaded=\"10\"");
        toReplace.put("retention_undownloaded=\"0\"", "retention_undownloaded=\"10\"");
        uploadPmode(SERVICE_PORT, toReplace);


        createMessage("msg1_found@domibus.eu",  Date.from(LocalDate.of(2024, 2, 20).atStartOfDay().toInstant(ZoneOffset.UTC)), MessageStatus.SEND_ENQUEUED);
        createMessage("msg2_found@domibus.eu",  Date.from(LocalDate.of(2024, 2, 20).atStartOfDay().toInstant(ZoneOffset.UTC)), MessageStatus.WAITING_FOR_RETRY);
        createMessage("msg3_found@domibus.eu",  Date.from(LocalDateTime.now().minusMinutes(120).toInstant(ZoneOffset.UTC)), MessageStatus.SEND_ENQUEUED);

        createMessage("msg1_Notfound@domibus.eu",  Date.from(LocalDateTime.now().minusMinutes(5).toInstant(ZoneOffset.UTC)), MessageStatus.SEND_ENQUEUED);

        final List<EArchiveBatchUserMessage> ongoingMessagesWhichAreNotProcessedAnymore = ongoingMessageSanitizingService.findOngoingMessagesWhichAreNotProcessedAnymore();
        assertEquals(3, ongoingMessagesWhichAreNotProcessedAnymore.size());
    }

    private void createMessage(String messageId, Date receivedDate, MessageStatus waitingForRetry) {
        UserMessageLog userMessageLog2 = messageDaoTestUtil.createUserMessageLog(
                messageId,
                receivedDate,
                waitingForRetry);
        long idPk = Long.parseLong(dateUtil.getIdPkDateHourPrefix(receivedDate)+ MIN) + counter ++;
        LOG.info("IDPK generated: [{}]", idPk);
        messageDaoTestUtil.updateUserMessageAndUserMessageLogPrimaryKey(userMessageLog2.getEntityId(), idPk);
    }
}
