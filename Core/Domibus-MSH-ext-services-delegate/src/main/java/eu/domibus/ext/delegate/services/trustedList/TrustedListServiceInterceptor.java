package eu.domibus.ext.delegate.services.trustedList;

import eu.domibus.ext.delegate.services.interceptor.ServiceInterceptor;
import eu.domibus.ext.exceptions.TrustedListExtException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 *
 * @author Breaz Ionut
 * @since 5.0.8
 */

@Aspect
@Component
public class TrustedListServiceInterceptor extends ServiceInterceptor {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TrustedListServiceInterceptor.class);

    @Around(value = "execution(public * eu.domibus.ext.delegate.services.trustedList.TrustedListService.*(..))")
    @Override
    public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable {
        return super.intercept(joinPoint);
    }

    @Override
    public Exception convertCoreException(Exception e) {
        return new TrustedListExtException(e);
    }

    @Override
    public DomibusLogger getLogger() {
        return LOG;
    }
}
