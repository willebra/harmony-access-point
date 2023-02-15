package eu.domibus.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.crypto.MultiDomainCryptoServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.TruststoreResource;
import eu.domibus.web.rest.ro.TrustStoreRO;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_TRUSTSTORE_LOCATION;
import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TruststoreResourceIT extends AbstractIT {
    public static final String KEYSTORES = "keystores";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TruststoreResourceIT.class);

    @Autowired
    private TruststoreResource truststoreResource;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Autowired
    private MultiDomainCryptoServiceImpl multiDomainCryptoService;

    @Autowired
    CertificateHelper certificateHelper;

    @Before
    public void before() {
        resetInitalTruststore();
    }

    @Test
    public void testTruststoreEntries_ok() throws IOException {
        List<TrustStoreRO> trustStoreROS = truststoreResource.trustStoreEntries();
        for (TrustStoreRO trustStoreRO : trustStoreROS) {
            Assert.assertNotNull("Certificate name should be populated in TrustStoreRO:", trustStoreRO.getName());
            Assert.assertNotNull("Certificate subject should be populated in TrustStoreRO:", trustStoreRO.getSubject());
            Assert.assertNotNull("Certificate issuer should be populated in TrustStoreRO:", trustStoreRO.getIssuer());
            Assert.assertNotNull("Certificate validity from should be populated in TrustStoreRO:", trustStoreRO.getValidFrom());
            Assert.assertNotNull("Certificate validity until should be populated in TrustStoreRO:", trustStoreRO.getValidUntil());
            Assert.assertNotNull("Certificate fingerprints should be populated in TrustStoreRO:", trustStoreRO.getFingerprints());
            Assert.assertNotNull("Certificate imminent expiry alert days should be populated in TrustStoreRO:", trustStoreRO.getCertificateExpiryAlertDays());
            Assert.assertEquals("Certificate imminent expiry alert days should be populated in TrustStoreRO:", 60, trustStoreRO.getCertificateExpiryAlertDays());
        }
    }

    @Test
    public void replaceTrustStore() throws IOException {
        String location = domibusPropertyProvider.getProperty(DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        String back = location.replace("gateway_truststore.jks", "gateway_truststore_back.jks");
        Files.copy(Paths.get(location), Paths.get(back), REPLACE_EXISTING);

        List<TrustStoreRO> entries = truststoreResource.trustStoreEntries();

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/gateway_truststore2.jks")) {
            MultipartFile multiPartFile = new MockMultipartFile("gateway_truststore2.jks", "gateway_truststore2.jks",
                    "octetstream", IOUtils.toByteArray(resourceAsStream));

            truststoreResource.uploadTruststoreFile(multiPartFile, "test123");

            List<TrustStoreRO> newEntries = truststoreResource.trustStoreEntries();

            Assert.assertNotEquals(entries.size(), newEntries.size());

            Files.copy(Paths.get(back), Paths.get(location), REPLACE_EXISTING);
            Files.delete(Paths.get(back));
        }
    }

    @Test
    public void replaceKeyStoreWithDifferentType() throws IOException {

        String fileName = "gateway_truststore_p12.p12";
        Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, fileName);
        byte[] content = Files.readAllBytes(path);

        MultipartFile multiPartFile = new MockMultipartFile(fileName, fileName,
                "octetstream", content);

        truststoreResource.uploadTruststoreFile(multiPartFile, "test123");

        List<TrustStoreRO> newEntries = truststoreResource.trustStoreEntries();

        Assert.assertEquals(1, newEntries.size());
    }

    @Test
    public void isChangedOnDisk() throws IOException {

        boolean changedOnDisk = truststoreResource.isChangedOnDisk();
        Assert.assertFalse(changedOnDisk);

        String location = domibusPropertyProvider.getProperty(DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        String back = location.replace("gateway_truststore.jks", "gateway_truststore_back.jks");
        String newLoc = location.replace("gateway_truststore.jks", "gateway_truststore2.jks");
        Files.copy(Paths.get(location), Paths.get(back), REPLACE_EXISTING);
        Files.copy(Paths.get(newLoc), Paths.get(location), REPLACE_EXISTING);
        Files.setLastModifiedTime(Paths.get(location), FileTime.from(new Date().toInstant()));

        changedOnDisk = truststoreResource.isChangedOnDisk();
        Assert.assertTrue(changedOnDisk);

        Files.copy(Paths.get(back), Paths.get(location), REPLACE_EXISTING);
        changedOnDisk = truststoreResource.isChangedOnDisk();
        Assert.assertFalse(changedOnDisk);

        Files.delete(Paths.get(back));
    }

    private void resetInitalTruststore() {
        try {
            String storePassword = "test123";
            Domain domain = DomainService.DEFAULT_DOMAIN;
            Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, "gateway_truststore_original.jks");
            byte[] content = Files.readAllBytes(path);
            KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, "gateway_truststore.jks", content, storePassword);
            multiDomainCryptoService.replaceTrustStore(domain, storeInfo);
        } catch (Exception e) {
            LOG.info("Error restoring initial keystore", e);
        }
    }
}
