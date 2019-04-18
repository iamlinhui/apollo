package com.ctrip.framework.apollo.spring.property;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.boot.bind.PropertiesConfigurationFactory;

import java.util.Collection;
import java.util.Map;

public class SpringBootConfigRegistry {

    private final Map<String, PropertiesConfigurationFactory<Object>> registry = Maps.newHashMap();

    public void register(String key, PropertiesConfigurationFactory<Object> springValue) {
        registry.put(key, springValue);
    }

    public Collection<PropertiesConfigurationFactory<Object>> get(String key) {
        Collection<PropertiesConfigurationFactory<Object>> collection = Lists.newArrayList();
        for (String registryKey : registry.keySet()) {
            if (key.indexOf(registryKey) == 0) {
                collection.add(registry.get(registryKey));
            }
        }
        return collection;
    }

}
