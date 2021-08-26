package eu.domibus.core.ebms3.receiver.policy;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.Messaging;
import eu.domibus.api.model.UserMessage;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.receiver.leg.ServerInMessageLegConfigurationFactory;
import eu.domibus.core.ebms3.ws.policy.PolicyService;
import eu.domibus.core.message.SoapService;
import eu.domibus.core.message.UserMessageHandlerService;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.property.DomibusVersionService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

/**
 * @author Catalin Enache, Soumya Chandran
 * @since 4.2
 */
@RunWith(JMockit.class)
public class SetPolicyInServerInterceptorTest {

    @Tested
    SetPolicyInServerInterceptor setPolicyInServerInterceptor;

    @Injectable
    BackendNotificationService backendNotificationService;

    @Injectable
    UserMessageHandlerService userMessageHandlerService;

    @Injectable
    SoapService soapService;

    @Injectable
    Ebms3Converter ebms3Converter;

    @Injectable
    protected PolicyService policyService;

    @Injectable
    protected DomibusVersionService domibusVersionService;

    @Injectable
    ServerInMessageLegConfigurationFactory serverInMessageLegConfigurationFactory;

    @Test
    @Ignore("EDELIVERY-8052 Failing tests must be ignored")
    public void processPluginNotification(final @Injectable EbMS3Exception ebMS3Exception,
                                          final @Injectable LegConfiguration legConfiguration,
                                          final @Injectable Ebms3Messaging messaging,
                                          final @Injectable UserMessage userMessage) {

        new Expectations(setPolicyInServerInterceptor) {{
            userMessageHandlerService.checkTestMessage(userMessage);
            result = false;

            legConfiguration.getErrorHandling().isBusinessErrorNotifyConsumer();
            result = true;
        }};

        //tested method
        setPolicyInServerInterceptor.processPluginNotification(ebMS3Exception, legConfiguration, messaging);

//        new FullVerifications(backendNotificationService) {{
//            backendNotificationService.notifyMessageReceivedFailure(userMessage, Matchers.eq(new ArrayList<>()), userMessageHandlerService.createErrorResult(ebMS3Exception));
//        }};
    }

    @Test
    public void logIncomingMessaging(final @Injectable SoapMessage soapMessage) throws Exception {

        //tested method
        setPolicyInServerInterceptor.logIncomingMessaging(soapMessage);

        new Verifications() {{
            soapService.getMessagingAsRAWXml(soapMessage);
        }};
    }

    @Test
    public void handleMessage(@Injectable SoapMessage message,
                              @Injectable HttpServletResponse response) throws JAXBException, IOException, EbMS3Exception {

        setPolicyInServerInterceptor.handleMessage(message);

        new Verifications() {{
            soapService.getMessage(message);
            times = 1;
            policyService.parsePolicy("policies" + File.separator + anyString);
            times = 1;
        }};
    }

    @Test(expected = Fault.class)
    public void handleMessageThrowsIOException(@Injectable SoapMessage message,
                                               @Injectable HttpServletResponse response
    ) throws JAXBException, IOException, EbMS3Exception {


        new Expectations() {{
            soapService.getMessage(message);
            result = new IOException();

        }};

        setPolicyInServerInterceptor.handleMessage(message);

        new FullVerifications() {{
            soapService.getMessage(message);
            policyService.parsePolicy("policies" + File.separator + anyString);
            setPolicyInServerInterceptor.setBindingOperation(message);
        }};
    }

    @Test(expected = Fault.class)
    public void handleMessageEbMS3Exception(@Injectable SoapMessage message,
                                            @Injectable HttpServletResponse response,
                                            @Injectable Messaging messaging) throws JAXBException, IOException, EbMS3Exception {

        new Expectations() {{
            soapService.getMessage(message);
            result = EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0010)
                    .message("no valid security policy found")
                    .build();
        }};
        setPolicyInServerInterceptor.handleMessage(message);

        new Verifications() {{
            soapService.getMessage(message);
            times = 1;
            policyService.parsePolicy("policies" + File.separator + anyString);
            times = 1;
            setPolicyInServerInterceptor.setBindingOperation(message);
            times = 1;
        }};
    }
}