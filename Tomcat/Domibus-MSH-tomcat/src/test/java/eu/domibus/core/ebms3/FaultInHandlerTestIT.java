package eu.domibus.core.ebms3;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.cxf.CxfCurrentMessageService;
import eu.domibus.core.ebms3.receiver.FaultInHandler;
import eu.domibus.core.property.PropertyProviderHelper;
import eu.domibus.test.AbstractIT;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.ws.handler.soap.SOAPMessageContext;

@Transactional
public class FaultInHandlerTestIT extends AbstractIT {

    @Autowired
    FaultInHandler faultInHandler;

    @Autowired
    DomainContextProvider domainContextProvider;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    PropertyProviderHelper propertyProviderHelper;

    //we are simulating a C2 which is sending a message to a C3 on multitenancy; C2 is sending a message to C3 to a domain which is not configured at C3; see EDELIVERY-12987
    @Test
    public void testHandleExceptionWhenC2SendsAMessageToAC3DomainAndTheDomainIsNotConfiguredAtC3() {
        domainContextProvider.clearCurrentDomain();

        Object saveCxfCurrentMessageServiceField = ReflectionTestUtils.getField(faultInHandler, "cxfCurrentMessageService");
        Object saveIsMultiTenantAwareField = ReflectionTestUtils.getField(propertyProviderHelper, "isMultiTenantAware");


        try {
            final Message message = Mockito.mock(Message.class);

            String messageId = "1";
            Mockito.when(message.getContextualProperty(Mockito.any())).thenReturn(messageId);
            Mockito.when(message.getExchange()).thenReturn(Mockito.mock(Exchange.class));

            final CxfCurrentMessageService cxfCurrentMessageServiceMock = Mockito.mock(CxfCurrentMessageService.class);
            Mockito.when(cxfCurrentMessageServiceMock.getCurrentMessage()).thenReturn(message);

            ReflectionTestUtils.setField(faultInHandler, "cxfCurrentMessageService", cxfCurrentMessageServiceMock);

            //we simulate multitenancy to reproduce the context of the bug
            ReflectionTestUtils.setField(propertyProviderHelper, "isMultiTenantAware", true);

            final SOAPMessageContext soapMessageContext = Mockito.mock(SOAPMessageContext.class);
            Mockito.when(soapMessageContext.get(Mockito.any())).thenReturn(new RuntimeException("Simulating an error"));

            faultInHandler.handleFault(soapMessageContext);
        } finally {
            //we put back the old values
            ReflectionTestUtils.setField(propertyProviderHelper, "isMultiTenantAware", saveIsMultiTenantAwareField);
            ReflectionTestUtils.setField(faultInHandler, "cxfCurrentMessageService", saveCxfCurrentMessageServiceField);
        }
    }
}
