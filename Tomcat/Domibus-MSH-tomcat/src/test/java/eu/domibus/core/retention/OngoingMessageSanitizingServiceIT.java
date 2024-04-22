package eu.domibus.core.retention;

import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.message.retention.OngoingMessageSanitizingService;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.test.AbstractIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

/**
 * @author François Gautier
 * @since 5.1
 */
public class OngoingMessageSanitizingServiceIT extends AbstractIT {

    @Autowired
    protected OngoingMessageSanitizingService ongoingMessageSanitizingService;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @Before
    public void updatePMode() throws IOException, XmlProcessingException {
        uploadPmode(SERVICE_PORT);
    }

    @Test
    public void findOngoingMessagesWhichAreNotProcessedAnymore() {

        List<EArchiveBatchUserMessage> ongoingMessagesWhichAreNotProcessedAnymore = ongoingMessageSanitizingService.findOngoingMessagesWhichAreNotProcessedAnymore();

        Assert.assertEquals(0, ongoingMessagesWhichAreNotProcessedAnymore.size());
    }
}
