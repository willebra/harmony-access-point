package eu.domibus.core.property;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyMetadata;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static eu.domibus.api.property.DomibusPropertyMetadata.NAME_SEPARATOR;
import static org.junit.Assert.assertEquals;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@RunWith(JMockit.class)
public class NestedPropertiesManagerTest {

    @Tested
    NestedPropertiesManager nestedPropertiesManager;

    @Injectable
    PropertyProviderHelper propertyProviderHelper;

    @Injectable
    ConfigurableEnvironment environment;

    @Test
    @Ignore("EDELIVERY-8052 Failing tests must be ignored")
    public void testGetNestedProperties(@Mocked DomibusPropertyMetadata prop) {
        String prefix = "routing";

        Set<String> propertiesStartingWithPrefix = new HashSet<>();
        propertiesStartingWithPrefix.add("routing.rule1");
        propertiesStartingWithPrefix.add("routing.rule1.queue");
        propertiesStartingWithPrefix.add("routing.rule1.service");

        new Expectations() {{
            prop.getName();
            result = prefix;
            propertyProviderHelper.filterPropertyNames((Predicate) any);
            result = propertiesStartingWithPrefix;
        }};

        List<String> nestedProperties = nestedPropertiesManager.getNestedProperties(prop);
        Assert.assertEquals(1, nestedProperties.size());
        Assert.assertTrue(nestedProperties.contains("rule1"));
    }

    @Test
    public void getPropertyPrefix(@Mocked DomibusPropertyMetadata prop) {
        String prefix = "domain1.rule1";

        new Expectations(nestedPropertiesManager) {{
            nestedPropertiesManager.computePropertyPrefix((Domain) any, prop);
            result = prefix;
        }};

        String propertyPrefix = nestedPropertiesManager.getPropertyPrefix(DomainService.DEFAULT_DOMAIN, prop);
        assertEquals(prefix + NAME_SEPARATOR, propertyPrefix);

        new Verifications() {{
            nestedPropertiesManager.computePropertyPrefix((Domain) any, prop);
        }};
    }

    @Test
    public void getPropertyPrefixForMultitenancy(@Mocked DomibusPropertyMetadata prop) {
        String prefix = "rule1";
        String domainPrefix = "default.rule1";
        String propertyPrefix = domainPrefix + ".";

        new Expectations(nestedPropertiesManager) {{
            nestedPropertiesManager.computePropertyPrefix(DomainService.DEFAULT_DOMAIN, prop);
            result = domainPrefix;
        }};

        String propertyName = nestedPropertiesManager.getPropertyPrefix(DomainService.DEFAULT_DOMAIN, prop);
        assertEquals(propertyPrefix, propertyName);
    }

    @Test
    public void computePropertyPrefixForDefaultDomain(@Mocked DomibusPropertyMetadata prop) {
        String propPrefix = "rule1";
        new Expectations() {{
            prop.getName();
            result = propPrefix;

            prop.isOnlyGlobal();
            result=false;

            prop.isDomain();
            result = true;

            propertyProviderHelper.isMultiTenantAware();
            result = true;

            propertyProviderHelper.getPropertyKeyForDomain(DomainService.DEFAULT_DOMAIN, propPrefix);
            result = DomainService.DEFAULT_DOMAIN.getCode() + NAME_SEPARATOR + propPrefix;
        }};

        String value = nestedPropertiesManager.computePropertyPrefix(DomainService.DEFAULT_DOMAIN, prop);

        Assert.assertEquals(DomainService.DEFAULT_DOMAIN.getCode() + NAME_SEPARATOR + propPrefix, value);
    }

    @Test
    public void computePropertyPrefix(@Injectable Domain domain, @Mocked DomibusPropertyMetadata prop) {
        String domainCode = "digit";
        String propPrefix = "propPrefix";

        new Expectations(nestedPropertiesManager) {{
            prop.getName();
            result = propPrefix;

            prop.isOnlyGlobal();
            result=false;

            prop.isDomain();
            result = true;

            propertyProviderHelper.isMultiTenantAware();
            result = true;

            propertyProviderHelper.getPropertyKeyForDomain(domain, propPrefix);
            result = domainCode + NAME_SEPARATOR + propPrefix;
        }};

        String value = nestedPropertiesManager.computePropertyPrefix(domain, prop);

        Assert.assertEquals(domainCode + NAME_SEPARATOR + propPrefix, value);
    }
}
