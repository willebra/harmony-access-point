package eu.domibus.core.earchive;

import eu.domibus.api.earchive.DomibusEArchiveService;
import eu.domibus.api.earchive.EArchiveBatchFilter;
import eu.domibus.api.earchive.EArchiveBatchRequestDTO;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.model.ListUserMessageDto;
import eu.domibus.api.model.UserMessageDTO;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.converter.EArchiveBatchMapper;
import eu.domibus.core.earchive.job.EArchiveBatchDispatcherService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static eu.domibus.api.model.DomibusDatePrefixedSequenceIdGeneratorGenerator.dateToPKUserMessageId;
import static eu.domibus.api.model.DomibusDatePrefixedSequenceIdGeneratorGenerator.extractDateFromPKUserMessageId;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_EARCHIVE_REST_API_RETURN_MESSAGES;

/**
 * @author François Gautier
 * @since 5.0
 */
@Service
public class EArchivingDefaultService implements DomibusEArchiveService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchivingDefaultService.class);

    public static final int CONTINUOUS_ID = 1;

    public static final int SANITY_ID = 2;

    private final EArchiveBatchDao eArchiveBatchDao;
    private final EArchiveBatchStartDao eArchiveBatchStartDao;
    private final EArchiveBatchMapper eArchiveBatchMapper;
    private final EArchiveBatchDispatcherService eArchiveBatchDispatcherService;
    private final DomainContextProvider domainContextProvider;
    private final DomibusPropertyProvider domibusPropertyProvider;

    public EArchivingDefaultService(EArchiveBatchDao eArchiveBatchDao,
                                    EArchiveBatchStartDao eArchiveBatchStartDao,
                                    EArchiveBatchMapper eArchiveBatchMapper,
                                    EArchiveBatchDispatcherService eArchiveBatchDispatcherService,
                                    DomainContextProvider domainContextProvider,
                                    DomibusPropertyProvider domibusPropertyProvider) {
        this.eArchiveBatchStartDao = eArchiveBatchStartDao;
        this.eArchiveBatchDao = eArchiveBatchDao;
        this.eArchiveBatchMapper = eArchiveBatchMapper;
        this.eArchiveBatchDispatcherService = eArchiveBatchDispatcherService;
        this.domainContextProvider = domainContextProvider;
        this.domibusPropertyProvider = domibusPropertyProvider;
    }

    @Override
    public void updateStartDateContinuousArchive(Long startMessageDate) {
        updateEArchiveBatchStart(CONTINUOUS_ID, startMessageDate);
    }

    @Override
    public void updateStartDateSanityArchive(Long startMessageDate) {
        updateEArchiveBatchStart(SANITY_ID, startMessageDate);
    }

    @Override
    public Long getStartDateContinuousArchive() {
        return extractDateFromPKUserMessageId(eArchiveBatchStartDao.findByReference(CONTINUOUS_ID).getLastPkUserMessage());
    }

    @Override
    public Long getStartDateSanityArchive() {
        return extractDateFromPKUserMessageId(eArchiveBatchStartDao.findByReference(SANITY_ID).getLastPkUserMessage());
    }

    private void updateEArchiveBatchStart(int sanityId, Long startMessageDate) {
        EArchiveBatchStart byReference = eArchiveBatchStartDao.findByReference(sanityId);
        byReference.setLastPkUserMessage(dateToPKUserMessageId(startMessageDate));
        eArchiveBatchStartDao.update(byReference);
    }

    @Override
    public Long getBatchRequestListCount(EArchiveBatchFilter filter) {
        return eArchiveBatchDao.getBatchRequestListCount(filter);
    }

    @Override
    public List<EArchiveBatchRequestDTO> getBatchRequestList(EArchiveBatchFilter filter) {

        Boolean returnMessages = domibusPropertyProvider.getBooleanProperty(DOMIBUS_EARCHIVE_REST_API_RETURN_MESSAGES);
        Class<? extends EArchiveBatchBaseEntity> getResultProjections = returnMessages ? EArchiveBatchEntity.class : EArchiveBatchBaseEntity.EArchiveBatchSummaryEntity.class;
        List<? extends EArchiveBatchBaseEntity> requestDTOList = eArchiveBatchDao.getBatchRequestList(filter, getResultProjections);
        return requestDTOList.stream().map(eArchiveBatchEntity ->
                eArchiveBatchMapper.eArchiveBatchRequestEntityToDto(eArchiveBatchEntity)
        ).collect(Collectors.toList());
    }

    @Override
    public ListUserMessageDto getBatchUserMessageList(String batchId, Integer pageStart, Integer pageSize) {
        List<UserMessageDTO> list = eArchiveBatchDao.getBatchMessageList(batchId, pageStart, pageSize);
        return new ListUserMessageDto(list);
    }

    @Override
    public Long getBatchUserMessageListCount(String batchId) {
        EArchiveBatchEntity batch = eArchiveBatchDao.findEArchiveBatchByBatchId(batchId);
        if (batch == null) {
            throw new DomibusEArchiveException("EArchive batch not found batchId: [" + batchId + "]");
        }
        return batch.getBatchSize() != null ? batch.getBatchSize().longValue() : 0L;
    }

    @Override
    public ListUserMessageDto getNotArchivedMessages(Date messageStartDate, Date messageEndDate, Integer pageStart, Integer pageSize) {
        List<UserMessageDTO> list = eArchiveBatchDao.getNotArchivedMessages(messageStartDate, messageEndDate, pageStart, pageSize);
        return new ListUserMessageDto(list);
    }

    @Override
    public EArchiveBatchRequestDTO reExportBatch(String batchId) {
        // create a copy of the  batch and submit it to JMS
        EArchiveBatchEntity copyBatch = eArchiveBatchDispatcherService.createBatchCopyAndEnqueue(batchId, domainContextProvider.getCurrentDomain());
        return eArchiveBatchMapper.eArchiveBatchRequestEntityToDto(copyBatch);
    }

    @Override
    public EArchiveBatchRequestDTO setBatchClientStatus(String batchId, EArchiveBatchStatus batchStatus) {
        EArchiveBatchEntity eArchiveBatchEntity = eArchiveBatchDao.findEArchiveBatchByBatchId(batchId);

        if (eArchiveBatchEntity == null) {
            throw new DomibusEArchiveException("EArchive batch not found batchId: [" + batchId + "]");
        }
        EArchiveBatchEntity result = eArchiveBatchDao.setStatus(eArchiveBatchEntity, batchStatus);
        return eArchiveBatchMapper.eArchiveBatchRequestEntityToDto(result);
    }
}
