package eu.domibus.core.alerts.configuration.manager;

import eu.domibus.core.alerts.configuration.reader.PluginPasswordImminentExpirationAlertConfigurationReader;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.service.ConfigurationLoader;
import eu.domibus.core.alerts.model.service.RepetitiveAlertModuleConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PluginPasswordImminentExpirationAlertConfigurationManager implements AlertConfigurationManager {

    @Autowired
    private ConfigurationLoader<RepetitiveAlertModuleConfiguration> loader;

    @Override
    public AlertType getAlertType() {
        return AlertType.PLUGIN_PASSWORD_IMMINENT_EXPIRATION;
    }

    @Override
    public RepetitiveAlertModuleConfiguration getConfiguration() {
        return loader.getConfiguration(new PluginPasswordImminentExpirationAlertConfigurationReader()::readConfiguration);
    }

    @Override
    public void reset() {
        loader.resetConfiguration();
    }
}
