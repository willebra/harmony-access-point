package eu.domibus.core.message;

import eu.domibus.api.datasource.AutoCloseFileDataSource;
import eu.domibus.api.model.*;
import eu.domibus.api.model.splitandjoin.MessageFragmentEntity;
import eu.domibus.api.model.splitandjoin.MessageGroupEntity;
import eu.domibus.core.message.splitandjoin.SplitAndJoinDefaultService;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import java.io.File;
import java.util.*;

/**
 * Factory for creating UserMessage instances
 *
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class UserMessageDefaultFactory implements UserMessageFactory {

    private static final List<String> ALLOWED_PROPERTIES = Arrays.asList(new String[]{"originalSender", "finalRecipient", "trackingIdentifier"});
    public static final String CID_FRAGMENT = "cid:fragment";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    protected PartPropertyDao partPropertyDao;
    protected MessagePropertyDao messagePropertyDao;
    protected PartyIdDao partyIdDao;
    protected PartyRoleDao partyRoleDao;

    public UserMessageDefaultFactory(PartPropertyDao partPropertyDao, MessagePropertyDao messagePropertyDao, PartyIdDao partyIdDao, PartyRoleDao partyRoleDao) {
        this.partPropertyDao = partPropertyDao;
        this.messagePropertyDao = messagePropertyDao;
        this.partyIdDao = partyIdDao;
        this.partyRoleDao = partyRoleDao;
    }

    @Override
    public UserMessage createUserMessageFragment(UserMessage sourceMessage, MessageGroupEntity messageGroupEntity, Long fragmentNumber, String fragmentFile) {
        UserMessage result = new UserMessage();
        result.setMessageFragment(true);
        String messageId = sourceMessage.getMessageId() + SplitAndJoinDefaultService.FRAGMENT_FILENAME_SEPARATOR + fragmentNumber;
        result.setMessageId(messageId);
        result.setRefToMessageId(sourceMessage.getRefToMessageId());
        result.setTimestamp(sourceMessage.getTimestamp());
        result.setConversationId(sourceMessage.getConversationId());
        result.setAgreementRef(createAgreementRef(sourceMessage));
        result.setAction(createAction(sourceMessage));
        result.setService(createService(sourceMessage));
        result.setPartyInfo(createPartyInfo(sourceMessage.getPartyInfo()));
        result.setMessageProperties(createMessageProperties(sourceMessage.getMessageProperties()));
//        result.setPayloadInfo(createPayloadInfo(fragmentFile, fragmentNumber));

//        MessageFragmentEntity messageFragmentEntity = createMessageFragmentEntity(messageGroupEntity, fragmentNumber);
//        result.setMessageFragment(messageFragmentEntity);

        return result;
    }

    @Override
    public UserMessage cloneUserMessageFragment(UserMessage userMessageFragment) {
        UserMessage result = new UserMessage();
        result.setMessageId(userMessageFragment.getMessageId());
        result.setRefToMessageId(userMessageFragment.getRefToMessageId());
        result.setTimestamp(userMessageFragment.getTimestamp());

        result.setConversationId(userMessageFragment.getConversationId());
        result.setAgreementRef(createAgreementRef(userMessageFragment));
        result.setAction(createAction(userMessageFragment));
        result.setService(createService(userMessageFragment));

        result.setPartyInfo(createPartyInfo(userMessageFragment.getPartyInfo()));
        result.setMessageProperties(createMessageProperties(userMessageFragment.getMessageProperties()));
        return result;
    }

    @Override
    public MessageFragmentEntity createMessageFragmentEntity(MessageGroupEntity messageGroupEntity, Long fragmentNumber) {
        MessageFragmentEntity result = new MessageFragmentEntity();
        result.setFragmentNumber(fragmentNumber);
        result.setGroup(messageGroupEntity);
        return result;
    }

    @Override
    public PartInfo createMessageFragmentPartInfo(String fragmentFile, Long fragmentNumber) {
        final PartInfo partInfo = new PartInfo();
        partInfo.setInBody(false);
        partInfo.setPayloadDatahandler(new DataHandler(new AutoCloseFileDataSource(fragmentFile)));
        partInfo.setHref(CID_FRAGMENT + fragmentNumber);
        partInfo.setFileName(fragmentFile);
        partInfo.setLength(new File(fragmentFile).length());

        PartProperty partProperty = partPropertyDao.findOrCreateProperty(Property.MIME_TYPE, APPLICATION_OCTET_STREAM, null);
        Set<PartProperty> partProperties = new HashSet<>();
        partProperties.add(partProperty);
        partInfo.setPartProperties(partProperties);
        return partInfo;
    }

    protected AgreementRefEntity createAgreementRef(UserMessage userMessage) {
        AgreementRefEntity agreementRef = userMessage.getAgreementRef();
        if (agreementRef == null) {
            return null;
        }

        AgreementRefEntity result = new AgreementRefEntity();
        result.setType(agreementRef.getType());
        result.setValue(agreementRef.getValue());
        return result;
    }

    protected ActionEntity createAction(UserMessage userMessage) {
        ActionEntity result = new ActionEntity();
        result.setValue(userMessage.getActionValue());
        return result;
    }

    protected ServiceEntity createService(UserMessage userMessage) {
        ServiceEntity result = new ServiceEntity();
        ServiceEntity userMessageService = userMessage.getService();
        result.setType(userMessageService.getType());
        result.setValue(userMessageService.getValue());
        return result;
    }

    protected PartyInfo createPartyInfo(final PartyInfo source) {
        final PartyInfo partyInfo = new PartyInfo();


        if (source.getFrom() != null) {
            final From from = new From();
            PartyRole fromPartyRole = partyRoleDao.findOrCreateRole(source.getFrom().getRole().getValue());
            from.setRole(fromPartyRole);

            PartyId fromPartyId = source.getFrom().getPartyId();
            PartyId fromParty = partyIdDao.findOrCreateParty(fromPartyId.getValue(), fromPartyId.getType());
            from.setPartyId(fromParty);

            partyInfo.setFrom(from);
        }

        if (source.getTo() != null) {
            final To to = new To();
            PartyRole toPartyRole = partyRoleDao.findOrCreateRole(source.getTo().getRole().getValue());
            to.setRole(toPartyRole);

            PartyId toPartyId = source.getTo().getPartyId();
            PartyId toParty = partyIdDao.findOrCreateParty(toPartyId.getValue(), toPartyId.getType());
            to.setPartyId(toParty);

            partyInfo.setTo(to);
        }

        return partyInfo;
    }

    protected Set<MessageProperty> createMessageProperties(final Collection<MessageProperty> userMessageProperties) {
        final Set<MessageProperty> messageProperties = new HashSet<>();

        for (MessageProperty sourceProperty : userMessageProperties) {
            if (ALLOWED_PROPERTIES.contains(sourceProperty.getName())) {
                MessageProperty messageProperty = messagePropertyDao.findOrCreateProperty(sourceProperty.getName(), sourceProperty.getValue(), sourceProperty.getType());
                messageProperties.add(messageProperty);
            }
        }

        return messageProperties;
    }
}
