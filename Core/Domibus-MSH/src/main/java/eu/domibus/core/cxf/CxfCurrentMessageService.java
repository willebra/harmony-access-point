package eu.domibus.core.cxf;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.springframework.stereotype.Service;

@Service
public class CxfCurrentMessageService {

    public Message getCurrentMessage() {
        return PhaseInterceptorChain.getCurrentMessage();
    }

}
