package eu.domibus.plugin.ws.exception;

import eu.domibus.messaging.MessageNotFoundException;

/**
 * @author G. Maier
 * @since 5.1.4
 */
public class WSMessageLogNotFoundException extends MessageNotFoundException {
    public WSMessageLogNotFoundException(String message) {
        super(message);
    }
}
