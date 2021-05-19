package eu.domibus.core.message.signal;

import eu.domibus.core.message.MessageLogInfoFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@Service(value = "signalMessageLogInfoFilter")
public class SignalMessageLogInfoFilter extends MessageLogInfoFilter {

    private static final String CONVERSATION_ID = "conversationId";

    private static final String EMPTY_CONVERSATION_ID = "'',";

    @Override
    protected String getHQLKey(String originalColumn) {
        if (StringUtils.equals(originalColumn, CONVERSATION_ID)) {
            return "";
        }
        if (StringUtils.equals(originalColumn, "messageId")) {
            return "signal.signalMessageId";
        }
        if (StringUtils.equals(originalColumn, "refToMessageId")) {
            return "signal.refToMessageId";
        }
        return super.getHQLKey(originalColumn);
    }

    @Override
    protected StringBuilder filterQuery(String query, String column, boolean asc, Map<String, Object> filters) {
        if (StringUtils.isNotEmpty(String.valueOf(filters.get(CONVERSATION_ID)))) {
            filters.put(CONVERSATION_ID, null);
        }
        return super.filterQuery(query, column, asc, filters);
    }

    @Override
    public String filterMessageLogQuery(String column, boolean asc, Map<String, Object> filters) {
        String query = "select new eu.domibus.core.message.MessageLogInfo(" +
                "signal.signalMessageId," +
                "log.messageStatus.messageStatus," +
                "log.mshRole.role," +
                "log.deleted," +
                "log.received," +
                EMPTY_CONVERSATION_ID +
                " partyFrom.value," +
                " partyTo.value," +
                (isFourCornerModel() ? " propsFrom.value," : "'',") +
                (isFourCornerModel() ? " propsTo.value," : "'',") +
                "signal.refToMessageId," +
                "message.testMessage" +
                ")" + getQueryBody(filters);
        StringBuilder result = filterQuery(query, column, asc, filters);
        return result.toString();
    }

    @Override
    public String getQueryBody(Map<String, Object> filters) {
        return
                " from SignalMessageLog log " +
                        "join log.signalMessage signal " +
                        "join signal.userMessage message " +
                        (isFourCornerModel() ?
                                "left join message.messageProperties propsFrom " +
                                        "left join message.messageProperties propsTo " : StringUtils.EMPTY) +
                        "left join message.partyInfo.from.partyId partyFrom " +
                        "left join message.partyInfo.to.partyId partyTo " +
                        (isFourCornerModel() ?
                                "where propsFrom.name = 'originalSender' " +
                                        "and propsTo.name = 'finalRecipient' " : StringUtils.EMPTY);
    }

    @Override
    protected String getMainTable() {
        return "SignalMessageLog log ";
    }

    @Override
    protected Map<String, List<String>> createFromMappings() {
        Map<String, List<String>> mappings = new HashMap<>();
        String messageTable = " join log.signalMessage signal join signal.userMessage message ";

        mappings.put("messaging", Arrays.asList(messageTable));
        mappings.put("message", Arrays.asList(messageTable));
        mappings.put("signal", Arrays.asList(messageTable));
        //  mappings.put("info", Arrays.asList(messageTable));
        mappings.put("propsFrom", Arrays.asList(messageTable, "left join message.messageProperties.property propsFrom "));
        mappings.put("propsTo", Arrays.asList(messageTable, "left join message.messageProperties.property propsTo "));
        mappings.put("partyFrom", Arrays.asList(messageTable, "left join message.partyInfo.from.partyId partyFrom "));
        mappings.put("partyTo", Arrays.asList(messageTable, "left join message.partyInfo.to.partyId partyTo "));
        return mappings;
    }

    @Override
    protected Map<String, List<String>> createWhereMappings() {
        Map<String, List<String>> mappings = new HashMap<>();
        String messageCriteria = "1=1"; // signal.messageInfo.messageId=log.messageId and signal.messageInfo.refToMessageId=message.messageInfo.messageId ";
        mappings.put("message", Arrays.asList(messageCriteria));
        mappings.put("signal", Arrays.asList(messageCriteria));
        mappings.put("propsFrom", Arrays.asList(messageCriteria, "and propsFrom.name = 'originalSender' "));
        mappings.put("propsTo", Arrays.asList(messageCriteria, "and propsTo.name = 'finalRecipient' "));
        return mappings;
    }
}
