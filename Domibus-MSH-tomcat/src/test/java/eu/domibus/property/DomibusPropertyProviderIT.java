package eu.domibus.property;

import eu.domibus.AbstractIT;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.cache.DomibusCacheService;
import eu.domibus.core.property.GlobalPropertyMetadataManager;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.property.ExternalTestModulePropertyManager.*;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
public class DomibusPropertyProviderIT extends AbstractIT {

    @Autowired
    org.springframework.cache.CacheManager cacheManager;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    GlobalPropertyMetadataManager globalPropertyMetadataManager;

    Domain defaultDomain = new Domain("default", "Default");

    @Test
    public void testCacheDomain() {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        //not in cache now
        String cachedValue = getCachedValue(defaultDomain, propertyName);
        //add to cache
        String actualValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);
        Assert.assertNotEquals(actualValue, cachedValue);

        //gets the cached value now
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assert.assertEquals(actualValue, cachedValue);
    }

    @Test
    public void testCacheNoDomain() {
        String propertyName = DOMIBUS_UI_REPLICATION_QUEUE_CONCURENCY;

        //not in cache now
        String cachedValue = getCachedValue(propertyName);
        //add to cache
        String actualValue = domibusPropertyProvider.getProperty(propertyName);
        Assert.assertNotEquals(actualValue, cachedValue);

        //gets the cached value now
        cachedValue = getCachedValue(propertyName);
        Assert.assertEquals(actualValue, cachedValue);
    }

    @Test
    public void testCacheEvict() {
        String propertyName = DOMIBUS_UI_SUPPORT_TEAM_NAME;

        String cachedValue = getCachedValue(defaultDomain, propertyName);
        Assert.assertNull(cachedValue);
        //add to cache
        String actualValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);
        //gets the cached value now
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assert.assertNotNull(cachedValue);
        Assert.assertEquals(cachedValue, actualValue);

        String newValue = actualValue + "MODIFIED";
        //evicts from cache
        domibusPropertyProvider.setProperty(defaultDomain, propertyName, newValue);
        //so not in cache
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assert.assertNull(cachedValue);

        //add to cache again
        actualValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);
        //finds it there
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assert.assertEquals(newValue, actualValue);
    }

    private String getCachedValue(Domain domain, String propertyName) {
        if (domain == null) {
            domain = domainContextProvider.getCurrentDomainSafely();
        }
        String domainCode = domain == null ? "global" : domain.getCode();

        String key = domainCode + ":" + propertyName;
        return cacheManager.getCache(DomibusCacheService.DOMIBUS_PROPERTY_CACHE).get(key, String.class);
    }

    private String getCachedValue(String propertyName) {
        return getCachedValue(null, propertyName);
    }

    @Test
    public void getPropertyValue_non_existing() {
        String propName = EXTERNAL_NOT_EXISTENT;
        DomibusPropertyMetadata result = globalPropertyMetadataManager.getPropertyMetadata(propName);
        Assert.assertEquals(DomibusPropertyMetadata.Usage.ANY.getValue(), result.getUsage());
    }

    @Test
    public void getPropertyValue_existing_storedLocally_notHandled() {
        String propName = EXTERNAL_MODULE_EXISTENT_NOT_HANDLED;
        Domain currentDomain = domainContextProvider.getCurrentDomain();
        String propValue = "true";

        String result = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assert.assertEquals(null, result);

        domibusPropertyProvider.setProperty(currentDomain, propName, propValue);
        String result2 = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assert.assertEquals(null, result2);
    }

    @Test
    public void getPropertyValue_existing_storedLocally_handled() {
        String propName = EXTERNAL_MODULE_EXISTENT_LOCALLY_HANDLED;
        Domain currentDomain = domainContextProvider.getCurrentDomain();
        String propValue = propName + ".value";

        String result = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assert.assertEquals(propValue, result);

        String newValue = "newValue";
        domibusPropertyProvider.setProperty(currentDomain, propName, newValue);
        String result2 = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assert.assertEquals(newValue, result2);
    }

    @Test
    public void getPropertyValue_existing_storedGlobally() {
        String propName = EXTERNAL_MODULE_EXISTENT_GLOBALLY_HANDLED;
        Domain currentDomain = domainContextProvider.getCurrentDomain();
        String propValue = null;

        String result = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assert.assertEquals(propValue, result);

        String newValue = "newValue";
        domibusPropertyProvider.setProperty(currentDomain, propName, newValue);
        String result2 = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assert.assertEquals(newValue, result2);
    }

    @Test
    public void getPropertyWithUTF8SpecialCharacters() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("properties/test.properties");
        String utf8String = "Message status change:PL|ąćęłńóżź|ALPHA: α |LATIN SMALL LETTER E WITH ACUTE:ê";
        Properties properties = new Properties();
        properties.setProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT, utf8String);
        properties.load(input);
        String mailSubject = properties.getProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT);

        //Default encoding for properties file reading is ISO-8859-1. So we get different value.
        Assert.assertNotEquals(mailSubject, utf8String);

        Domain currentDomain = domainContextProvider.getCurrentDomain();
        domibusPropertyProvider.setProperty(currentDomain, mailSubject, utf8String);
        String uft8MailSubject = domibusPropertyProvider.getProperty(currentDomain, mailSubject);

        //Domibus property configuration set the encoding to UTF8-8 . So we get same string with utf8 characters.
        Assert.assertEquals(uft8MailSubject, utf8String);
        input.close();
    }
}
