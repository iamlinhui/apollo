package com.ctrip.framework.apollo.spring.property;

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * 针对configproperties的自动更新监听
 */
public class ApolloAutoUpdateConfigChangeListener implements ConfigChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(ApolloAutoUpdateConfigChangeListener.class);

    private final Environment environment;
    private final SpringBootConfigRegistry springValueRegistry;

    public ApolloAutoUpdateConfigChangeListener(Environment environment) {
        this.environment = environment;
        this.springValueRegistry = SpringInjector.getInstance(SpringBootConfigRegistry.class);
    }

    @Override
    public void onChange(ConfigChangeEvent changeEvent) {
        Set<String> keys = changeEvent.changedKeys();
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        for (String key : keys) {
            // 1. check whether the changed key is relevant
            Collection<PropertiesConfigurationFactory<Object>> targetValues = springValueRegistry.get(key);
            if (targetValues == null || targetValues.isEmpty()) {
                continue;
            }

            // 2. check whether the value is really changed or not (since spring property sources have hierarchies)
            if (!shouldTriggerAutoUpdate(changeEvent, key)) {
                continue;
            }

            // 3. update the value
            for (PropertiesConfigurationFactory<Object> val : targetValues) {
                try {
                    val.bindPropertiesToTarget();
                } catch (BindException e) {
                    logger.error("bindPropertiesToTarget error", e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Check whether we should trigger the auto update or not.
     * <br />
     * For added or modified keys, we should trigger auto update if the current value in Spring equals to the new value.
     * <br />
     * For deleted keys, we will trigger auto update anyway.
     */
    private boolean shouldTriggerAutoUpdate(ConfigChangeEvent changeEvent, String changedKey) {
        ConfigChange configChange = changeEvent.getChange(changedKey);

        if (configChange.getChangeType() == PropertyChangeType.DELETED) {
            return true;
        }

        return Objects.equals(environment.getProperty(changedKey), configChange.getNewValue());
    }

}
