package eu.domibus.tomcat.jpa;

import com.zaxxer.hikari.HikariDataSource;
import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.tomcat.environment.NoH2DatabaseCondition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_SECOND;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Conditional(NoH2DatabaseCondition.class)
@Configuration
public class TomcatDatasourceConfiguration {

    @Bean(name = DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE, destroyMethod = "close")
    public DataSource domibusDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        return getDomibusDataSource(domibusPropertyProvider);
    }

    @Bean(name = DataSourceConstants.DOMIBUS_JDBC_QUARTZ_DATA_SOURCE, destroyMethod = "close")
    public DataSource quartzDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        return getQuartzDataSource(domibusPropertyProvider);
    }

    private HikariDataSource getDomibusDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getHikariDataSource(domibusPropertyProvider, DOMIBUS_DATASOURCE_DRIVER_CLASS_NAME, DOMIBUS_DATASOURCE_URL, DOMIBUS_DATASOURCE_USER,
                DOMIBUS_DATASOURCE_PASSWORD, DOMIBUS_DATASOURCE_MAX_LIFETIME, DOMIBUS_DATASOURCE_MAX_POOL_SIZE, DOMIBUS_DATASOURCE_CONNECTION_TIMEOUT,
                DOMIBUS_DATASOURCE_IDLE_TIMEOUT, DOMIBUS_DATASOURCE_MINIMUM_IDLE, DOMIBUS_DATASOURCE_POOL_NAME);
    }

    private HikariDataSource getQuartzDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getHikariDataSource(domibusPropertyProvider, DOMIBUS_QUARTZ_DATASOURCE_DRIVER_CLASS_NAME, DOMIBUS_QUARTZ_DATASOURCE_URL,
                DOMIBUS_QUARTZ_DATASOURCE_USER, DOMIBUS_QUARTZ_DATASOURCE_PASSWORD, DOMIBUS_QUARTZ_DATASOURCE_MAX_LIFETIME,
                DOMIBUS_QUARTZ_DATASOURCE_MAX_POOL_SIZE, DOMIBUS_QUARTZ_DATASOURCE_CONNECTION_TIMEOUT, DOMIBUS_QUARTZ_DATASOURCE_IDLE_TIMEOUT,
                DOMIBUS_QUARTZ_DATASOURCE_MINIMUM_IDLE, DOMIBUS_QUARTZ_DATASOURCE_POOL_NAME);
    }

    private HikariDataSource getHikariDataSource(DomibusPropertyProvider domibusPropertyProvider, String datasourceDriverClassName, String datasourceUrl,
                                                 String datasourceUser, String datasourcePassword, String datasourceMaxLifetime, String datasourceMaxPoolSize,
                                                 String datasourceConnectionTimeout, String datasourceIdleTimeout, String datasourceMinimumIdle, String datasourcePoolName) {
        HikariDataSource dataSource = new HikariDataSource();
        final String driverClassName = domibusPropertyProvider.getProperty(datasourceDriverClassName);
        dataSource.setDriverClassName(driverClassName);

        final String dataSourceURL = domibusPropertyProvider.getProperty(datasourceUrl);
        dataSource.setJdbcUrl(dataSourceURL);

        final String user = domibusPropertyProvider.getProperty(datasourceUser);
        dataSource.setUsername(user);

        final String password = domibusPropertyProvider.getProperty(datasourcePassword); //NOSONAR
        dataSource.setPassword(password);

        final Integer maxLifetimeInSecs = domibusPropertyProvider.getIntegerProperty(datasourceMaxLifetime);
        dataSource.setMaxLifetime(maxLifetimeInSecs * MILLIS_PER_SECOND);

        final Integer maxPoolSize = domibusPropertyProvider.getIntegerProperty(datasourceMaxPoolSize);
        dataSource.setMaximumPoolSize(maxPoolSize);

        final Integer connectionTimeout = domibusPropertyProvider.getIntegerProperty(datasourceConnectionTimeout);
        dataSource.setConnectionTimeout(connectionTimeout * MILLIS_PER_SECOND);

        final Integer idleTimeout = domibusPropertyProvider.getIntegerProperty(datasourceIdleTimeout);
        dataSource.setIdleTimeout(idleTimeout * MILLIS_PER_SECOND);

        final Integer minimumIdle = domibusPropertyProvider.getIntegerProperty(datasourceMinimumIdle);
        dataSource.setMinimumIdle(minimumIdle);

        final String poolName = domibusPropertyProvider.getProperty(datasourcePoolName);
        if (!StringUtils.isBlank(poolName)) {
            dataSource.setPoolName(poolName);
        }

        return dataSource;
    }
}
