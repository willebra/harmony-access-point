package eu.domibus.core.message.test;

import eu.domibus.api.model.PartyId;
import eu.domibus.core.message.dictionary.PartyIdDao;
import eu.domibus.core.message.testservice.TestService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.test.AbstractIT;
import eu.domibus.web.rest.ro.TestServiceMessageInfoRO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Transactional
public class TestServiceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(TestServiceIT.class);

    @Autowired
    private TestService testService;

    @Autowired
    PartyIdDao partyIdDao;

    @Before
    public void before() {
        try {
            uploadPmode(18001);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Transactional
    public void submitTest() throws MessagingProcessingException, IOException {
        String pModePartyType = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";
        String anotherPartyType = "urn:oasis:names:tc:ebcore:partyid-type:eudamed";
        String senderParty = "domibus-blue";
        String receiverParty = "domibus-red";

        partyIdDao.findOrCreateParty(senderParty, pModePartyType);
        partyIdDao.findOrCreateParty(receiverParty, pModePartyType);

        partyIdDao.findOrCreateParty(senderParty, anotherPartyType);
        partyIdDao.findOrCreateParty(receiverParty, anotherPartyType);

        List<PartyId> parties = partyIdDao.findAll();

        testService.submitTest(senderParty, receiverParty);

        parties = partyIdDao.findAll();

        TestServiceMessageInfoRO res = testService.getLastTestSentWithErrors(senderParty, receiverParty);
        assertNotNull(res);
        assertNull(res.getErrorInfo());
    }


}
