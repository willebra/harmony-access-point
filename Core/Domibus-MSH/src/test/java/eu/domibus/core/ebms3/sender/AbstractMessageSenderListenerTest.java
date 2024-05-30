package eu.domibus.core.ebms3.sender;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.message.UserMessageDefaultService;
import eu.domibus.core.util.DateUtilImpl;
import eu.domibus.logging.DomibusLogger;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.JMSException;
import javax.jms.Message;

import java.util.Date;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES;

/**
 * @author Sebastian-Ion TINCU
 * @since 5.1.4
 */
@RunWith(JMockit.class)
public class AbstractMessageSenderListenerTest {

    @Tested(availableDuringSetup = true)
    private AbstractMessageSenderListener abstractMessageSenderListener;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private MessageSenderService messageSenderService;

    @Injectable
    private UserMessageDefaultService userMessageService;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DateUtilImpl dateUtil;

    @Injectable
    private DomibusLogger domibusLogger;

    @Injectable
    private Message message;

    @Before
    public void setup() {
        new Expectations() {{
            abstractMessageSenderListener.getLogger();
            result = domibusLogger;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_debugLogDisabled() throws Exception {
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = false;
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            times = 0;
            message.getJMSTimestamp();
            times = 0;
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_zeroMinutesDuration() throws Exception {
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = 0;
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.debug("Maximum allowed time in minutes is currently ignored [{}]", 0);
            message.getJMSTimestamp();
            times = 0;
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_negativeDuration() throws Exception {
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = -100;
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.debug("Maximum allowed time in minutes is currently ignored [{}]", -100);
            message.getJMSTimestamp();
            times = 0;
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_JMSException() throws Exception {
        final JMSException jmsException = new JMSException("");
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = 1;
            message.getJMSTimestamp();
            result = jmsException;
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.debug("Error getting the message JMS timestamp for [{}]", anyString, jmsException);
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_zeroJMSTimestamp() throws Exception {
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = 1;
            message.getJMSTimestamp();
            result = 0;
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_negativeJMSTimestamp() throws Exception {
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = 1;
            message.getJMSTimestamp();
            result = -100;
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }


    @Test
    public void validateEnqueuedMessageDuration_messageEnqueuedForLessThanConfiguredDuration() throws Exception {
        final Date now = new Date();
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = 7;
            message.getJMSTimestamp();
            result = now.getTime() + 1000;  // 1 second into the future
            dateUtil.getDateMinutesAgo(7);
            result = now;                   // now
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_messageEnqueuedForExactlySameConfiguredDuration() throws Exception {
        final Date now = new Date();
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = 7;
            message.getJMSTimestamp();
            result = now.getTime();         // now
            dateUtil.getDateMinutesAgo(7);
            result = now;                   // now
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
            times = 0;
        }};
    }

    @Test
    public void validateEnqueuedMessageDuration_messageEnqueuedForMoreThanConfiguredDuration() throws Exception {
        final Date now = new Date();
        new Expectations() {{
            domibusLogger.isDebugEnabled();
            result = true;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_LOGGING_SEND_MESSAGE_ENQUEUED_MAX_MINUTES);
            result = 7;
            message.getJMSTimestamp();
            result = now.getTime() - 1000;         // now
            dateUtil.getDateMinutesAgo(7);
            result = now;                   // now
        }};

        abstractMessageSenderListener.validateEnqueuedMessageDuration(message, "");

        new FullVerifications() {{
            domibusLogger.debug("Validating the enqueued duration for [{}]", anyString);
            domibusLogger.warn("User message [{}] has been enqueued for more than the maximum allowed time of [{}] minutes", anyString, anyInt);
        }};
    }
}