package eu.domibus.ext.exceptions;

/**
 * Trusted List refresh Exception
 *
 * @author Breaz Ionut
 * @since 5.0.8
 */
public class TrustedListExtException extends DomibusServiceExtException {

    public TrustedListExtException(DomibusErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public TrustedListExtException(DomibusErrorCode errorCode, String message, Throwable throwable) {
        super(errorCode, message, throwable);
    }

    public TrustedListExtException(Throwable cause) {
        this(DomibusErrorCode.DOM_001, cause.getMessage(), cause);
    }

    public TrustedListExtException(String message) {
        this(DomibusErrorCode.DOM_001, message);
    }

}

