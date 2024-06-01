package eu.domibus;

import eu.domibus.api.jms.JMSManager;
import eu.domibus.core.alerts.listener.AlertListener;
import eu.domibus.core.alerts.listener.PluginEvenListener;
import eu.domibus.core.alerts.listener.generic.DefaultEventListener;
import eu.domibus.core.alerts.listener.generic.FrequencyEventListener;
import eu.domibus.core.alerts.listener.generic.RepetitiveEventListener;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.jms.JMSManagerImpl;
import eu.domibus.ext.delegate.services.multitenancy.DomainContextServiceDelegate;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import javax.jms.Queue;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static eu.domibus.core.alerts.service.AlertServiceImpl.ALERT_SELECTOR;
import static eu.domibus.jms.spi.InternalJMSConstants.ALERT_MESSAGE_QUEUE;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@Configuration
public class JmsConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(JmsConfiguration.class);

    @Autowired
    @Lazy
    FrequencyEventListener frequencyEventListener;

    @Autowired
    @Lazy
    RepetitiveEventListener repetitiveEventListener;

    @Autowired
    @Lazy
    AlertListener alertListener;

    @Autowired
    @Lazy
    DefaultEventListener defaultEventListener;

    @Autowired
    @Lazy
    PluginEvenListener pluginEvenListener;

    @Autowired
    DomainContextServiceDelegate domainContextServiceDelegate;

    @Primary
    @Bean
    JMSManager jmsManager(@Qualifier(ALERT_MESSAGE_QUEUE) Queue alertMessageQueue) {
        return new JMSManagerImpl() {
            @Override
            public void convertAndSendToQueue(final Object message, final Queue destination, final String selector) {
                if (destination == alertMessageQueue) {

                    Consumer<Object> objectConsumer = getConsumer(selector);
                    if (objectConsumer != null) {
                        LOG.debug("Override jms standard behaviour for destination [{}] and selector [{}]", destination, selector);
                        objectConsumer.accept(message);
                        return;
                    }

                }
                LOG.debug("No jms override");
                super.convertAndSendToQueue(message, destination, selector);

            }
        };
    }

    private Consumer<Object> getConsumer(String selector) {
        Map<String, Consumer<Object>> map = new HashMap<>();
        String currentDomainCode = domainContextServiceDelegate.getCurrentDomainSafely().getCode();
        map.put(ALERT_SELECTOR,
                o -> alertListener.onAlert((Alert) o, currentDomainCode));
        map.put(EventType.QueueSelectors.DEFAULT,
                o -> defaultEventListener.onEvent((Event) o, currentDomainCode));
        map.put(EventType.QueueSelectors.FREQUENCY,
                o -> frequencyEventListener.onEvent((Event) o, currentDomainCode));
        map.put(EventType.QueueSelectors.REPETITIVE,
                o -> repetitiveEventListener.onEvent((Event) o, currentDomainCode));
        map.put(EventType.QueueSelectors.PLUGIN_EVENT,
                o -> pluginEvenListener.onPluginEvent((Event) o, currentDomainCode));

        return map.get(selector);
    }

}
