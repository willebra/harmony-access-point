package eu.domibus.core.message;

import eu.domibus.api.model.*;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.core.plugin.routing.RoutingService;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.handler.MessageSubmitter;
import eu.domibus.test.AbstractIT;
import eu.domibus.test.ITTestsService;
import eu.domibus.test.common.SubmissionUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import static eu.domibus.api.ebms3.model.Ebms3Property.MIME_TYPE;
import static eu.domibus.api.model.MessageStatus.RECEIVED;
import static eu.domibus.messaging.MessageConstants.COMPRESSION_PROPERTY_KEY;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertNotNull;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@Transactional
public class PartInfoTestIT extends AbstractIT {
    public static final String MPC = "UserMessageLogDaoITMpc";

    @Autowired
    MessagesLogServiceImpl messagesLogService;

    @Autowired
    ITTestsService itTestsService;

    @Autowired
    PartInfoDao partInfoDao;

    @Autowired
    protected SubmissionUtil submissionUtil;

    @Autowired
    MessageSubmitter messageSubmitter;

    @Autowired
    protected RoutingService routingService;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @Autowired
    DateUtil dateUtil;

    @Autowired
    BackendConnectorProvider backendConnectorProvider;

    private Date before;
    private Date timeT;
    private Date after;
    private Date old;

    private final String deletedNoProperties = randomUUID().toString();
    private final String deletedWithProperties = randomUUID().toString();
    private final String receivedNoProperties = randomUUID().toString();
    private final String receivedWithProperties = randomUUID().toString();
    private final String downloadedNoProperties = randomUUID().toString();
    private final String downloadedWithProperties = randomUUID().toString();
    private final String waitingForRetryNoProperties = randomUUID().toString();
    private final String waitingForRetryWithProperties = randomUUID().toString();
    private final String sendFailureNoProperties = randomUUID().toString();
    private final String sendFailureWithProperties = randomUUID().toString();
    private final String testDate = randomUUID().toString();
    private long maxEntityId;
    private UserMessageLog msg1;
    private UserMessageLog msg2;
    private UserMessageLog msg3;

    @Before
    @Transactional
    public void before() throws IOException, XmlProcessingException {
        BackendConnector backendConnector = Mockito.mock(BackendConnector.class);
        Mockito.when(backendConnectorProvider.getBackendConnector(Mockito.any(String.class))).thenReturn(backendConnector);


        uploadPmode();

        addMessages();
    }

    private void addMessages() {
        before = dateUtil.fromString("2019-01-01T12:00:00Z");
        timeT = dateUtil.fromString("2020-01-01T12:00:00Z");
        after = dateUtil.fromString("2021-01-01T12:00:00Z");
        old = Date.from(before.toInstant().minusSeconds(60 * 60 * 24)); // one day older than "before"

        msg1 = messageDaoTestUtil.createUserMessageLog("msg1-" + UUID.randomUUID(), timeT);
        msg2 = messageDaoTestUtil.createUserMessageLog("msg2-" + randomUUID(), timeT);
        msg3 = messageDaoTestUtil.createUserMessageLog("msg3-" + UUID.randomUUID(), old);

        messageDaoTestUtil.createUserMessageLog(testDate, Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant()), MSHRole.RECEIVING, MessageStatus.NOT_FOUND, true, MPC, new Date());

        messageDaoTestUtil.createUserMessageLog(deletedNoProperties, timeT, MSHRole.SENDING, MessageStatus.DELETED, false, MPC, new Date());
        messageDaoTestUtil.createUserMessageLog(receivedNoProperties, timeT, MSHRole.SENDING, RECEIVED, false, MPC, new Date());
        messageDaoTestUtil.createUserMessageLog(downloadedNoProperties, timeT, MSHRole.SENDING, MessageStatus.DOWNLOADED, false, MPC, new Date());
        messageDaoTestUtil.createUserMessageLog(waitingForRetryNoProperties, timeT, MSHRole.SENDING, MessageStatus.WAITING_FOR_RETRY, false, MPC, new Date());
        messageDaoTestUtil.createUserMessageLog(sendFailureNoProperties, timeT, MSHRole.SENDING, MessageStatus.SEND_FAILURE, false, MPC, new Date());

        messageDaoTestUtil.createUserMessageLog(deletedWithProperties, timeT, MSHRole.SENDING, MessageStatus.DELETED, "sender1", "recipient1");
        messageDaoTestUtil.createUserMessageLog(receivedWithProperties, timeT, MSHRole.SENDING, RECEIVED, "sender1", "recipient1");
        messageDaoTestUtil.createUserMessageLog(downloadedWithProperties, timeT, MSHRole.SENDING, MessageStatus.DOWNLOADED, "sender1", "recipient1");
        messageDaoTestUtil.createUserMessageLog(waitingForRetryWithProperties, timeT, MSHRole.SENDING, MessageStatus.WAITING_FOR_RETRY, "sender1", "recipient1");
        messageDaoTestUtil.createUserMessageLog(sendFailureWithProperties, timeT, MSHRole.SENDING, MessageStatus.SEND_FAILURE, "sender1", "recipient1");
    }

    @Test
    public void testPartInfoWhenSend() throws MessagingProcessingException, IOException {
        Submission submission = submissionUtil.createSubmission();
        uploadPmode();
        final String messageId = messageSubmitter.submit(submission, "mybackend");

        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId);
        assertNotNull(userMessageLog);

        List<PartInfo> partInfos = partInfoDao.findPartInfoByUserMessageEntityId(userMessageLog.getEntityId());
        Assert.assertEquals(1, partInfos.size());
        Set<PartProperty> partProperties = partInfos.get(0).getPartProperties();
        Assert.assertEquals(2, partProperties.size());
        List<PartProperty> partPropertiesList = new ArrayList<>(partProperties);
        Assert.assertEquals(true, partPropertiesList.get(0).getName().equals(MIME_TYPE));
        Assert.assertEquals(true, partPropertiesList.get(1).getName().equals(COMPRESSION_PROPERTY_KEY));
    }

    @Test
    @Transactional
    public void findUserMessageById() throws Exception {

        itTestsService.receiveMessage("msg1");

//        MessageLogRO result = messagesLogService.findUserMessageById("msg1");
//        Assert.assertNotNull(result);

        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId("msg1");
        List<PartInfo> partInfos = partInfoDao.findPartInfoByUserMessageEntityId(userMessageLog.getEntityId());
        Assert.assertEquals(1, partInfos.size());
        Set<PartProperty> partProperties = partInfos.get(0).getPartProperties();
        Assert.assertEquals(2, partProperties.size());
        List<PartProperty> partPropertiesList = new ArrayList<>(partProperties);
        PartProperty partProperty1 = partPropertiesList.get(0);
        Assert.assertEquals(true, partProperty1.getName().equals("description"));
        PartProperty partProperty2 = partPropertiesList.get(1);
        Assert.assertEquals(true, partProperty2.getName().equals(MIME_TYPE));
    }

}
