package eu.domibus.ext.delegate.services.trustedList;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.ext.services.TrustedListExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 *
 * Service that refreshes the Trusted Lists in DSS
 *
 * @author Breaz Ionut
 * @since 5.0.8
 */

@Service
public class TrustedListService implements TrustedListExtService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TrustedListService.class);

    protected DomainContextProvider domainContextProvider;
    protected MultiDomainCryptoService multiDomainCryptoService;

     public TrustedListService(MultiDomainCryptoService multiDomainCryptoService, DomainContextProvider domainContextProvider) {
        this.multiDomainCryptoService = multiDomainCryptoService;
        this.domainContextProvider = domainContextProvider;
     }

    @Override
    public void refreshTrustedLists() {
        LOG.info("Executing refresh trusted lists at: [{}]", LocalDateTime.now());
        multiDomainCryptoService.refreshTrustedLists(domainContextProvider.getCurrentDomain());
    }
}
