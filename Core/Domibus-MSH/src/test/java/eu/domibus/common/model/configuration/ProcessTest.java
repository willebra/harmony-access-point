package eu.domibus.common.model.configuration;

import mockit.integration.junit4.JMockit;
import org.hibernate.collection.internal.PersistentSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author  Joze Ritharsic
 * @since 4.2
 */
@RunWith(JMockit.class)
public class ProcessTest {

    @Test
    public void detachParties_testUpdateValues() {
        // given
        Party partyOne = new Party();
        partyOne.setName("partyOne");
        Party partyTwo = new Party();
        partyOne.setName("partyTwo");
        Party partyThree = new Party();
        partyThree.setName("partyThree");

        PersistentSet  initiatorParties = new PersistentSet();
        PersistentSet  responderParties = new PersistentSet();
        ReflectionTestUtils.setField(initiatorParties,"initialized",true);
        ReflectionTestUtils.setField(responderParties,"initialized",true);
        ReflectionTestUtils.setField(initiatorParties,"set",new HashSet());
        ReflectionTestUtils.setField(responderParties,"set",new HashSet());
        initiatorParties.add(partyOne);
        initiatorParties.add(partyTwo);
        responderParties.add(partyThree);


        Process testInstance = new Process();
        ReflectionTestUtils.setField(testInstance,"initiatorParties",initiatorParties);
        ReflectionTestUtils.setField(testInstance,"responderParties",responderParties);
        assertTrue(testInstance.getInitiatorParties() instanceof PersistentSet );
        assertTrue(testInstance.getResponderParties() instanceof PersistentSet);
        // when
        testInstance.detachParties();

        //then
        assertTrue(testInstance.getInitiatorParties() instanceof HashSet );
        assertTrue(testInstance.getResponderParties() instanceof HashSet);

        assertEquals(2, testInstance.getInitiatorParties().size());
        assertTrue(testInstance.getInitiatorParties().contains(partyOne));
        assertTrue(testInstance.getInitiatorParties().contains(partyTwo));
        assertEquals(1, testInstance.getResponderParties().size());
        assertTrue(testInstance.getResponderParties().contains(partyThree));
    }
}
