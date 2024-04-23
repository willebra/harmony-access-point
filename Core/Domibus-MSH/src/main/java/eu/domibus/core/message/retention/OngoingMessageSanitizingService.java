package eu.domibus.core.message.retention;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DateUtil;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static eu.domibus.api.model.DomibusDatePrefixedSequenceIdGeneratorGenerator.MIN;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ONGOING_MESSAGES_SANITIZING_WORKER_DELAY_HOURS;

/**
 * @author Cosmin Baciu
 * @since 5.1.4
 */
@Service
public class OngoingMessageSanitizingService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(OngoingMessagesSanitizingWorker.class);

    private final MessageRetentionPartitionsService messageRetentionPartitionsService;

    private final DateUtil dateUtil;

    private final UserMessageLogDao userMessageLogDao;

    private final DomibusPropertyProvider domibusPropertyProvider;

    public OngoingMessageSanitizingService(MessageRetentionPartitionsService messageRetentionPartitionsService,
                                           DateUtil dateUtil,
                                           UserMessageLogDao userMessageLogDao,
                                           DomibusPropertyProvider domibusPropertyProvider) {
        this.messageRetentionPartitionsService = messageRetentionPartitionsService;
        this.dateUtil = dateUtil;
        this.userMessageLogDao = userMessageLogDao;
        this.domibusPropertyProvider = domibusPropertyProvider;
    }

    public List<EArchiveBatchUserMessage> findOngoingMessagesWhichAreNotProcessedAnymore() {
        long lastMessageId = Long.MAX_VALUE;

        int maxRetention = getMaxRetention();
        if(maxRetention > 0) {
            Date lastDateNotBeingProcessed = DateUtils.addMinutes(dateUtil.getUtcDate(), maxRetention * -1);
            String idPrefix = dateUtil.getIdPkDateHourPrefix(lastDateNotBeingProcessed);
            lastMessageId = Long.parseLong(idPrefix + MIN);
            LOG.debug("Last Message Id: [{}]", lastMessageId);
        }
        List<EArchiveBatchUserMessage> messagesNotFinalAsc = userMessageLogDao.findMessagesNotFinalAsc(0, lastMessageId);
        return messagesNotFinalAsc;
    }

    /**
     * @return maximum value between the maxRetention and the retry timeout
     */
    private int getMaxRetention() {
        int maxRetention = messageRetentionPartitionsService.getMaxRetention();
        long retryTimeoutMin = getSanitizerDelayHours() * 60;
        return Integer.max((int) retryTimeoutMin, maxRetention);
    }

    protected long getSanitizerDelayHours() {
        Long delay = domibusPropertyProvider.getLongProperty(DOMIBUS_ONGOING_MESSAGES_SANITIZING_WORKER_DELAY_HOURS);
        if (delay == null) {
            LOG.debug("No value found for [{}]. Use no further delay", DOMIBUS_ONGOING_MESSAGES_SANITIZING_WORKER_DELAY_HOURS);
            return 0L;
        }
        return delay;
    }

}
