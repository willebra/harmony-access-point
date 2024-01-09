package eu.domibus;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.proxy.DomibusProxyService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.common.JPAConstants;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.core.crypto.TruststoreDao;
import eu.domibus.core.crypto.TruststoreEntity;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.dictionary.StaticDictionaryService;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.spring.DomibusApplicationContextListener;
import eu.domibus.core.spring.DomibusRootConfiguration;
import eu.domibus.core.user.ui.UserRoleDao;
import eu.domibus.core.util.WarningUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.test.common.DomibusTestDatasourceConfiguration;
import eu.domibus.web.spring.DomibusWebConfiguration;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.SocketUtils;
import org.w3c.dom.Document;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.awaitility.Awaitility.with;

/**
 * Created by feriaad on 02/02/2016.
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PropertyOverrideContextInitializer.class,
        classes = {DomibusRootConfiguration.class, DomibusWebConfiguration.class,
                DomibusTestDatasourceConfiguration.class, DomibusTestMocksConfiguration.class})
public abstract class AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractIT.class);

    protected static final int SERVICE_PORT = 8892;

    public static final String TEST_PLUGIN_USERNAME = "admin";

    public static final String TEST_PLUGIN_PASSWORD = "123456";

    @Autowired
    protected DomibusApplicationContextListener domibusApplicationContextListener;

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Autowired
    protected AuthUtils authUtils;

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusProxyService domibusProxyService;

    @Autowired
    protected TruststoreDao truststoreDao;

    @Autowired
    protected UserRoleDao userRoleDao;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @Autowired
    protected ITTestsService itTestsService;

    @Autowired
    UserMessageDao userMessageDao;

    public static boolean springContextInitialized = false;



    @BeforeClass
    public static void init() throws IOException {
        springContextInitialized = false;
        LOG.info(WarningUtil.warnOutput("Initializing Spring context"));

        FileUtils.deleteDirectory(new File("target/temp"));
        final File domibusConfigLocation = new File("target/test-classes");
        final String absolutePath = domibusConfigLocation.getAbsolutePath();
        System.setProperty("domibus.config.location", absolutePath);

        //we are using a new ActiveMQ broker for each SpringContext instantiation
        String activeMQBrokerName = "localhost" + SocketUtils.findAvailableTcpPort(61616, 62690);
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_TRANSPORT_CONNECTOR_URI, "vm://" + activeMQBrokerName + "?broker.persistent=false&create=false");
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_BROKER_NAME, activeMQBrokerName);
        LOG.info("activeMQBrokerName=[{}]", activeMQBrokerName);
    }

    @Before
    public void setDomain() {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);

        setAuth();

        if (!springContextInitialized) {
            LOG.info("Executing the ApplicationContextListener initialization");
            try {
                int secondsToSleep = 5;
                LOG.info("Waiting for database initialization: sleeping for [{}] seconds", secondsToSleep);
                //we need to wait a bit until all schemas and tables are completely initialized; the code below which waits for the records to be created in each schema is not enough for the database to be fully initialized
                Thread.sleep(secondsToSleep * 1000L);
                LOG.info("Finished waiting for database initialization");

                waitUntilDatabaseIsInitialized();
                domibusApplicationContextListener.initializeForTests();
            } catch (Exception ex) {
                LOG.warn("Domibus Application Context initialization failed", ex);
            } finally {
                springContextInitialized = true;
            }
        }
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
    }

    protected void setAuth() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test_user",
                        "test_password",
                        Collections.singleton(new SimpleGrantedAuthority(eu.domibus.api.security.AuthRole.ROLE_ADMIN.name()))));
    }


    protected void uploadPmode(Integer redHttpPort) throws IOException, XmlProcessingException {
        uploadPmode(redHttpPort, null);
    }

    protected void uploadPmode(Integer redHttpPort, Map<String, String> toReplace) throws IOException, XmlProcessingException {
        uploadPmode(redHttpPort, "dataset/pmode/PModeTemplate.xml", toReplace);
    }

    protected void uploadPmode(Integer redHttpPort, String pModeFilepath, Map<String, String> toReplace) throws IOException, XmlProcessingException {
        final InputStream inputStream = new ClassPathResource(pModeFilepath).getInputStream();

        String pmodeText = IOUtils.toString(inputStream, UTF_8);
        if (toReplace != null) {
            pmodeText = replace(pmodeText, toReplace);
        }
        if (redHttpPort != null) {
            LOG.info("Using wiremock http port [{}]", redHttpPort);
            pmodeText = pmodeText.replace(String.valueOf(SERVICE_PORT), String.valueOf(redHttpPort));
        }

        final Configuration pModeConfiguration = pModeProvider.getPModeConfiguration(pmodeText.getBytes(UTF_8));
        configurationDAO.updateConfiguration(pModeConfiguration);
        pModeProvider.refresh();
    }

    protected void uploadPmode() throws IOException, XmlProcessingException {
        uploadPmode(null);
    }

    protected UserMessage getUserMessageTemplate() throws IOException {
        Resource userMessageTemplate = new ClassPathResource("dataset/messages/UserMessageTemplate.json");
        String jsonStr = new String(IOUtils.toByteArray(userMessageTemplate.getInputStream()), UTF_8);
        return new ObjectMapper().readValue(jsonStr, UserMessage.class);

    }


    private void waitUntilDatabaseIsInitialized() {
        with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(120, TimeUnit.SECONDS).until(databaseIsInitialized());
    }

    protected void waitUntilMessageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(5, TimeUnit.SECONDS)
                .until(messageHasStatus(messageId, mshRole, messageStatus));
    }

    protected void waitUntilMessageIsAcknowledged(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.SENDING, MessageStatus.ACKNOWLEDGED);
    }

    protected void waitUntilMessageIsReceived(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.RECEIVING, MessageStatus.RECEIVED);
    }

    protected void waitUntilMessageIsInWaitingForRetry(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.SENDING, MessageStatus.WAITING_FOR_RETRY);
    }

    protected Callable<Boolean> messageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        return () -> messageStatus == userMessageLogDao.getMessageStatus(messageId, mshRole);
    }

    private Callable<Boolean> databaseIsInitialized() {
        return () -> {
            try {
                return userRoleDao.listRoles().size() > 0;
            } catch (Exception e) {
                LOG.error("Could not get the roles list", e);
            }
            return false;
        };
    }

    /**
     * Convert the given file to a string
     */
    protected String getAS4Response(String file) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream is = getClass().getClassLoader().getResourceAsStream("dataset/as4/" + file);
            Document doc = db.parse(is);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = null;
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (Exception exc) {
            Assert.fail(exc.getMessage());
            exc.printStackTrace();
        }
        return null;
    }

    public void prepareSendMessage(String responseFileName) {
        prepareSendMessage(responseFileName, null);
    }

    public void prepareSendMessage(String responseFileName, Map<String, String> toReplace) {
        String body = getAS4Response(responseFileName);
        if (toReplace != null) {
            body = replace(body, toReplace);
        }

        // Mock the response from the recipient MSH
        stubFor(post(urlEqualTo("/domibus/services/msh"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/soap+xml")
                        .withBody(body)));
    }

    protected String replace(String body, Map<String, String> toReplace) {
        for (String key : toReplace.keySet()) {
            body = body.replaceAll(key, toReplace.get(key));
        }
        return body;
    }

    protected void createStore(String storeName, String filePath) {
        if (truststoreDao.existsWithName(storeName)) {
            LOG.info("truststore already created");
            return;
        }
        LOG.info("create truststore [{}]", storeName);
        try {
            TruststoreEntity domibusTruststoreEntity = new TruststoreEntity();
            domibusTruststoreEntity.setName(storeName);
            domibusTruststoreEntity.setType("JKS");
            domibusTruststoreEntity.setPassword("test123");
            try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(filePath)) {
                byte[] trustStoreBytes = IOUtils.toByteArray(resourceAsStream);
                domibusTruststoreEntity.setContent(trustStoreBytes);
                truststoreDao.create(domibusTruststoreEntity);
            }
        } catch (Exception ex) {
            LOG.info("Error creating store entity [{}]", storeName, ex);
        }
    }

    protected void deleteAllMessages() {
        final List<UserMessage> userMessages = userMessageDao.findAll();
        if (CollectionUtils.isEmpty(userMessages)) {
            return;
        }
        final List<String> userMessageIds = userMessages.stream().map(userMessage -> userMessage.getMessageId()).collect(Collectors.toList());
        deleteAllMessages(userMessageIds.toArray(new String[0]));
    }

    public void deleteAllMessages(String... messageIds) {
        itTestsService.deleteAllMessages(messageIds);
    }

}
