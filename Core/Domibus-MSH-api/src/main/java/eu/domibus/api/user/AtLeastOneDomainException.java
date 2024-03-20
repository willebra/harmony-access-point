package eu.domibus.api.user;

/**
 * Raised when a user does not have any associated domains
 *
 * @author Ion Perpegel
 * @since 5.0.8
 */
public class AtLeastOneDomainException extends UserManagementException {
    public AtLeastOneDomainException() {
        super("The user does not have any associated domains. Please check the configuration.");
    }
}
