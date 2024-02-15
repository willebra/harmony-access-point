package eu.domibus.test;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.api.proxy.DomibusProxyService;
import eu.domibus.core.spring.DomibusApplicationContextListener;
import eu.domibus.core.spring.DomibusRootConfiguration;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.test.common.DomibusTestDatasourceConfiguration;
import eu.domibus.web.spring.DomibusWebConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.SocketUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PropertyOverrideContextInitializer.class,
        classes = {DomibusRootConfiguration.class, DomibusWebConfiguration.class,
                DomibusTestDatasourceConfiguration.class, DomibusTestMocksConfiguration.class})
public abstract class AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractIT.class);

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusProxyService domibusProxyService;

    @Autowired
    DomibusApplicationContextListener domibusApplicationContextListener;

    @Autowired
    protected DomibusConditionUtil domibusConditionUtil;
    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    private static boolean springContextInitialized = false;

    @BeforeClass
    public static void init() throws IOException {
        if(springContextInitialized) {
            return;
        }

        final File domibusConfigLocation = new File("target/test-classes");
        System.setProperty("domibus.config.location", domibusConfigLocation.getAbsolutePath());

        copyActiveMQFile(domibusConfigLocation);
        copyResourceFolder(domibusConfigLocation, "abstractIT/keystores", "keystores");
        copyResourceFolder(domibusConfigLocation, "abstractIT/policies", "policies");
        copyDomibusProperties(domibusConfigLocation);
        copyDomainResourceFolders(domibusConfigLocation);

        FileUtils.deleteDirectory(new File("target/temp"));

        //we are using randomly available port in order to allow run in parallel
        int activeMQConnectorPort = SocketUtils.findAvailableTcpPort(2000, 3100);
        int activeMQBrokerPort = SocketUtils.findAvailableTcpPort(61616, 62690);
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_CONNECTOR_PORT, String.valueOf(activeMQConnectorPort)); // see EDELIVERY-10294 and check if this can be removed
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_TRANSPORT_CONNECTOR_URI, "vm://localhost:" + activeMQBrokerPort + "?broker.persistent=false&create=false"); // see EDELIVERY-10294 and check if this can be removed
        LOG.info("activeMQBrokerPort=[{}]", activeMQBrokerPort);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test_user",
                        "test_password",
                        Collections.singleton(new SimpleGrantedAuthority(eu.domibus.api.security.AuthRole.ROLE_ADMIN.name()))));

        springContextInitialized = true;
    }

    @Before
    public void setDomain() {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        domibusConditionUtil.waitUntilDatabaseIsInitialized();

        if(!springContextInitialized) {
            LOG.info("Executing the ApplicationContextListener initialization");
            domibusApplicationContextListener.doInitialize();
        }
    }

    private static void copyDomibusProperties(File domibusConfigLocation) throws IOException {
        final File destDomibusPropertiesFile = new File(domibusConfigLocation, "domibus.properties");
        try (InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream("abstractIT/domibus.properties")) {
            if (inputStream != null) {
                Files.copy(inputStream, destDomibusPropertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void copyActiveMQFile(File domibusConfigLocation) throws IOException {
        final File internalDirectory = new File(domibusConfigLocation, "internal");
        FileUtils.forceMkdir(internalDirectory);
        final File destActiveMQ = new File(internalDirectory, "activemq.xml");
        try (InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream("abstractIT/activemq.xml")) {
            if (inputStream != null) {
                Files.copy(inputStream, destActiveMQ.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void copyDomainResourceFolders(File domibusConfigLocation) throws IOException {
        final String[] folders = {"domains",
                "domains/default", "domains/default/encrypt", "domains/default/keystores",
                "domains/red", "domains/red/encrypt", "domains/red/keystores"};

        for (String folder : folders) {
            copyResourceFolder(domibusConfigLocation, folder, folder);
        }
    }

    public static void copyResourceFolder(File domibusConfigLocation, String resourceFolder, String destFolder) throws IOException {
        final File destDirectory = new File(domibusConfigLocation, destFolder);
        FileUtils.forceMkdir(destDirectory);
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final Resource[] resourceList = resolver.getResources("classpath*:" + resourceFolder + "/*");
        for (Resource resource : resourceList) {
            final String fileName = resource.getFilename();
            final String url = resource.getURL().toString();
            // copy just resources from domibus-msh-test
            if (fileName != null && url.contains("domibus-msh-test")) {
                final File destinationPath = new File(destDirectory, fileName);
                Files.copy(resource.getInputStream(), destinationPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }}
