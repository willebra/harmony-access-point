package eu.domibus.core.crypto;

import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.cache.DomibusCacheService;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.crypto.api.DomainCryptoService;
import eu.domibus.core.crypto.api.DomainCryptoServiceFactory;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.security.auth.callback.CallbackHandler;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_KEYSTORE_LOCATION;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_TRUSTSTORE_LOCATION;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Service
public class MultiDomainCryptoServiceImpl implements MultiDomainCryptoService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MultiDomainCryptoServiceImpl.class);

    protected volatile Map<Domain, DomainCryptoService> domainCertificateProviderMap = new HashMap<>();

    @Autowired
    DomainCryptoServiceFactory domainCertificateProviderFactory;

    @Autowired
    private DomibusCacheService domibusCacheService;

    @Autowired
    private CertificateHelper certificateHelper;

    @Override
    public X509Certificate[] getX509Certificates(Domain domain, CryptoType cryptoType) throws WSSecurityException {
        LOG.debug("Get certificates for domain [{}] and cryptoType [{}]", domain, cryptoType);
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getX509Certificates(cryptoType);
    }

    @Override
    public String getX509Identifier(Domain domain, X509Certificate cert) throws WSSecurityException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getX509Identifier(cert);
    }

    @Override
    public PrivateKey getPrivateKey(Domain domain, X509Certificate certificate, CallbackHandler callbackHandler) throws WSSecurityException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getPrivateKey(certificate, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(Domain domain, PublicKey publicKey, CallbackHandler callbackHandler) throws WSSecurityException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getPrivateKey(publicKey, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(Domain domain, String identifier, String password) throws WSSecurityException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getPrivateKey(identifier, password);
    }

    @Override
    public void verifyTrust(Domain domain, X509Certificate[] certs, boolean enableRevocation, Collection<Pattern> subjectCertConstraints, Collection<Pattern> issuerCertConstraints) throws WSSecurityException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        domainCertificateProvider.verifyTrust(certs, enableRevocation, subjectCertConstraints, issuerCertConstraints);
    }

    @Override
    public void verifyTrust(Domain domain, PublicKey publicKey) throws WSSecurityException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        domainCertificateProvider.verifyTrust(publicKey);
    }

    @Override
    public String getDefaultX509Identifier(Domain domain) throws WSSecurityException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getDefaultX509Identifier();
    }

    @Override
    public String getPrivateKeyPassword(Domain domain, String privateKeyAlias) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getPrivateKeyPassword(privateKeyAlias);
    }

    @Override
    public void refreshTrustStore(Domain domain) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        domainCertificateProvider.refreshTrustStore();
    }

    @Override
    public void replaceTrustStore(Domain domain, String storeFileName, byte[] store, String password) throws CryptoException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        certificateHelper.validateStoreType(domainCertificateProvider.getTrustStoreType(), storeFileName);
        domainCertificateProvider.replaceTrustStore(store, password);
        domibusCacheService.clearCache("certValidationByAlias");
    }

    @Override
    public KeyStore getKeyStore(Domain domain) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getKeyStore();
    }

    @Override
    public KeyStore getTrustStore(Domain domain) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getTrustStore();
    }

    @Override
    @Cacheable(value = "certValidationByAlias", key = "#domain.code + #alias")
    public boolean isCertificateChainValid(Domain domain, String alias) throws DomibusCertificateException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.isCertificateChainValid(alias);
    }

    @Override
    public X509Certificate getCertificateFromKeystore(Domain domain, String alias) throws KeyStoreException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getCertificateFromKeyStore(alias);
    }

    @Override
    public boolean addCertificate(Domain domain, X509Certificate certificate, String alias, boolean overwrite) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.addCertificate(certificate, alias, overwrite);
    }

    @Override
    public void addCertificate(Domain domain, List<CertificateEntry> certificates, boolean overwrite) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        domainCertificateProvider.addCertificate(certificates, overwrite);
    }

    @Override
    public X509Certificate getCertificateFromTruststore(Domain domain, String alias) throws KeyStoreException {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getCertificateFromTrustStore(alias);
    }

    @Override
    public boolean removeCertificate(Domain domain, String alias) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.removeCertificate(alias);
    }

    @Override
    public void removeCertificate(Domain domain, List<String> aliases) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        domainCertificateProvider.removeCertificate(aliases);
    }

    protected DomainCryptoService getDomainCertificateProvider(Domain domain) {
        LOG.debug("Get domain CertificateProvider for domain [{}]", domain);
        if (domainCertificateProviderMap.get(domain) == null) {
            synchronized (domainCertificateProviderMap) {
                if (domainCertificateProviderMap.get(domain) == null) { //NOSONAR: double-check locking
                    LOG.debug("Creating domain CertificateProvider for domain [{}]", domain);
                    DomainCryptoService domainCertificateProvider = domainCertificateProviderFactory.createDomainCryptoService(domain);
                    domainCertificateProviderMap.put(domain, domainCertificateProvider);
                }
            }
        }
        return domainCertificateProviderMap.get(domain);
    }

    @Override
    public void reset() {
        domainCertificateProviderMap.values().stream().forEach(service -> service.reset());
    }

    @Override
    public void reset(Domain domain) {
        if (domain == null) {
            throw new InvalidParameterException("Domain is null.");
        }

        final DomainCryptoService domainCertificateProvider = domainCertificateProviderMap.get(domain);
        if (domainCertificateProvider == null) {
            throw new DomibusCertificateException("Domain certificate provider for domain [" + domain.getName() + "] not found.");
        }

        domainCertificateProvider.reset();
    }

    @Override
    public byte[] getTruststoreContent(Domain domain) {
        final DomainCryptoService domainCertificateProvider = getDomainCertificateProvider(domain);
        return domainCertificateProvider.getTruststoreContent();
    }

    @Autowired
    private DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected CertificateService certificateService;

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected DomainTaskExecutor domainTaskExecutor;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    TruststoreDao truststoreDao;

    @Override
    public byte[] getTruststoreContentFromFile(Domain domain) {
        String location = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        return certificateService.getTruststoreContentFromFile(location);
    }

    public byte[] getKeystoreContentFromFile(Domain domain) {
        String location = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEYSTORE_LOCATION);
        return certificateService.getTruststoreContentFromFile(location);
    }

    public void persistTruststoresIfApplicable() {
        LOG.debug("Creating encryption key for all domains if not yet exists");

        final List<Domain> domains = domainService.getDomains();
        for (Domain domain : domains) {
            persistTruststoreIfApplicable(domain);
        }

        LOG.debug("Finished creating encryption key for all domains if not yet exists");
    }

    private void persistTruststoreIfApplicable(Domain domain) {
        domainTaskExecutor.submit(() -> {
            persistCurrentDomainTruststoreIfApplicable();
            persistCurrentDomainKeystoreIfApplicable();
        }, domain);
    }

    public final static String DomibusTruststore = "domibus.truststore";
    public final static String DomibusKeystore = "domibus.keystore";

    private void persistCurrentDomainTruststoreIfApplicable() {
        if (truststoreDao.existsWithName(DomibusTruststore)) {
            return;
        }

        byte[] content = null;
        try {
            content = getTruststoreContentFromFile(domainContextProvider.getCurrentDomainSafely());
        } catch (DomibusCertificateException ex) {
            LOG.warn("Could not get trustsore content from file.", ex);
            return;
        }

        Truststore entity = new Truststore();
        entity.setType(DomibusTruststore);
        entity.setContent(content);
        truststoreDao.create(entity);
    }

    private void persistCurrentDomainKeystoreIfApplicable() {
        if (truststoreDao.existsWithName(DomibusKeystore)) {
            return;
        }

        byte[] content = null;
        try {
            content = getKeystoreContentFromFile(domainContextProvider.getCurrentDomainSafely());
        } catch (DomibusCertificateException ex) {
            LOG.warn("Could not get keysore content from file.", ex);
            return;
        }

        Truststore entity = new Truststore();
        entity.setType(DomibusKeystore);
        entity.setContent(content);
        truststoreDao.create(entity);
    }
}
