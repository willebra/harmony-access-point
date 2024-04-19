package eu.domibus.core.plugin.notification;

import eu.domibus.common.*;
import eu.domibus.core.plugin.delegate.BackendConnectorDelegate;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.plugin.BackendConnector;
import org.springframework.stereotype.Service;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Service
public class PluginMessageReceivedNotifier implements PluginEventNotifier <DeliverMessageEvent> {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PluginMessageReceivedNotifier.class);

    protected BackendConnectorDelegate backendConnectorDelegate;

    public PluginMessageReceivedNotifier(BackendConnectorDelegate backendConnectorDelegate) {
        this.backendConnectorDelegate = backendConnectorDelegate;
    }

    @Override
    public boolean canHandle(NotificationType notificationType) {
        return NotificationType.MESSAGE_RECEIVED == notificationType;
    }

    @Override
    public void notifyPlugin(DeliverMessageEvent messageEvent, BackendConnector<?, ?> backendConnector) {
        LOG.businessInfo(DomibusMessageCode.BUS_NOTIFY_MESSAGE_RECEIVED, messageEvent.getMessageId());
        backendConnectorDelegate.deliverMessage(backendConnector, messageEvent);
    }
}
