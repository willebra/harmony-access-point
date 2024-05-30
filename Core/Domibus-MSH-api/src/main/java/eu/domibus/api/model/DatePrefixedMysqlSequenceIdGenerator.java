package eu.domibus.api.model;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static eu.domibus.logging.DomibusLogger.MDC_DOMAIN;

/**
 * New sequence format generator. The method generates a new sequence using current date and a fixed length (10 digits) increment.
 *
 * @author idragusa
 * @since 5.0
 */
public class DatePrefixedMysqlSequenceIdGenerator extends DomibusTableGenerator
        implements DomibusDatePrefixedSequenceIdGeneratorGenerator {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(DatePrefixedMysqlSequenceIdGenerator.class);

    @Override
    protected String getCurrentDomain(SharedSessionContractImplementor session) {
        return LOGGER.getMDC(MDC_DOMAIN);
    }

}
