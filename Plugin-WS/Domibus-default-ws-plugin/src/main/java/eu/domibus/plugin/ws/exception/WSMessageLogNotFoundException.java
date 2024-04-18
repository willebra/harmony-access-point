package eu.domibus.plugin.ws.exception;

import eu.domibus.messaging.MessageNotFoundException;

public class WSMessageLogNotFoundException extends MessageNotFoundException {
    public WSMessageLogNotFoundException(String message) {
        super(message);
    }
}
