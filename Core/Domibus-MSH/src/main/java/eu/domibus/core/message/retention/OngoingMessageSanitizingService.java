package eu.domibus.core.message.retention;

import eu.domibus.api.util.DateUtil;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.message.UserMessageLogDao;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static eu.domibus.api.model.DomibusDatePrefixedSequenceIdGeneratorGenerator.MIN;

/**
 * @author Cosmin Baciu
 * @since 5.1.4
 */
@Service
public class OngoingMessageSanitizingService {

    private MessageRetentionPartitionsService messageRetentionPartitionsService;

    private DateUtil dateUtil;

    private UserMessageLogDao userMessageLogDao;

    public OngoingMessageSanitizingService(MessageRetentionPartitionsService messageRetentionPartitionsService, DateUtil dateUtil, UserMessageLogDao userMessageLogDao) {
        this.messageRetentionPartitionsService = messageRetentionPartitionsService;
        this.dateUtil = dateUtil;
        this.userMessageLogDao = userMessageLogDao;
    }

    public List<EArchiveBatchUserMessage> findOngoingMessagesWhichAreNotProcessedAnymore() {
        int maxRetention = messageRetentionPartitionsService.getMaxRetention();
        long lastMessageId = Long.MAX_VALUE;
        if(maxRetention > 0) {
            Date lastDateNotBeingProcessed = DateUtils.addMinutes(dateUtil.getUtcDate(), maxRetention * -1);
            String idPrefix = dateUtil.getIdPkDateHourPrefix(lastDateNotBeingProcessed);
            lastMessageId = Long.parseLong(idPrefix + MIN);
        }
        List<EArchiveBatchUserMessage> messagesNotFinalAsc = userMessageLogDao.findMessagesNotFinalAsc(0, lastMessageId);
        return messagesNotFinalAsc;
    }
}
