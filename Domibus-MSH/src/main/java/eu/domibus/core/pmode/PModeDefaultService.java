package eu.domibus.core.pmode;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.NotificationStatus;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.pmode.*;
import eu.domibus.api.pmode.domain.LegConfiguration;
import eu.domibus.api.pmode.domain.ReceptionAwareness;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.MessageExchangeService;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Service
public class PModeDefaultService implements PModeService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PModeDefaultService.class);

    @Autowired
    UserMessageDao userMessageDao;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private MessageExchangeService messageExchangeService;

    @Autowired
    PModeValidationHelper pModeValidationHelper;

    @Override
    public LegConfiguration getLegConfiguration(String messageId) {
        final UserMessage userMessage = userMessageDao.findByMessageId(messageId);
        boolean isPull = messageExchangeService.forcePullOnMpc(userMessage);
        String pModeKey;
        try {
            pModeKey = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, isPull).getPmodeKey();
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not get the PMode key for message [" + messageId + "]. Pull [" + isPull + "]", e);
        }
        eu.domibus.common.model.configuration.LegConfiguration legConfigurationEntity = pModeProvider.getLegConfiguration(pModeKey);
        return convert(legConfigurationEntity);
    }

    @Override
    public byte[] getPModeFile(long id) {
        return pModeProvider.getPModeFile(id);
    }

    @Override
    public PModeArchiveInfo getCurrentPMode() {
        return pModeProvider.getCurrentPmode();
    }

    @Transactional
    @Override
    public List<ValidationIssue> updatePModeFile(byte[] bytes, String description) throws PModeValidationException {
        try {
            return pModeProvider.updatePModes(bytes, description);
        } catch (XmlProcessingException e) {
            LOG.warn("Xml processing issue while trying to upload pmode with description [{}]", description, e);
            throw pModeValidationHelper.getPModeValidationException(e, "Failed to upload the PMode file due to: ");
        }
    }

    @Override
    public String findPartyName(String partyId, String partyIdType) {
        try {
            return pModeProvider.findPartyName(partyId, partyIdType);
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not find party for id:[" + partyId + "] and type:[" + partyIdType + "]", e);
        }

    }

    @Override
    public String findActionName(String action) {
        try {
            return pModeProvider.findActionName(action);
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not find action for value:[" + action + "]", e);
        }
    }

    @Override
    public String findServiceName(String service, String serviceType) {
        try {
            return pModeProvider.findServiceName(service, serviceType);
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not find service:[" + service + "] with type:[" + serviceType + "]", e);
        }
    }

    @Override
    public String findMpcName(String mpc) {
        try {
            return pModeProvider.findMpcName(mpc);
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not find mpc:[" + mpc + "]", e);
        }

    }

    protected LegConfiguration convert(eu.domibus.common.model.configuration.LegConfiguration legConfigurationEntity) {
        if (legConfigurationEntity == null) {
            return null;
        }
        final LegConfiguration result = new LegConfiguration();
        result.setReceptionAwareness(convert(legConfigurationEntity.getReceptionAwareness()));
        return result;
    }

    protected ReceptionAwareness convert(eu.domibus.common.model.configuration.ReceptionAwareness receptionAwarenessEntity) {
        if (receptionAwarenessEntity == null) {
            return null;
        }
        ReceptionAwareness result = new ReceptionAwareness();
        result.setDuplicateDetection(receptionAwarenessEntity.getDuplicateDetection());
        result.setName(receptionAwarenessEntity.getName());
        result.setRetryCount(receptionAwarenessEntity.getRetryCount());
        result.setRetryTimeout(receptionAwarenessEntity.getRetryTimeout());
        return result;
    }

    public NotificationStatus getNotificationStatus(eu.domibus.common.model.configuration.LegConfiguration legConfiguration) {
        return legConfiguration.getErrorHandling().isBusinessErrorNotifyProducer() ? NotificationStatus.REQUIRED : NotificationStatus.NOT_REQUIRED;
    }
}
