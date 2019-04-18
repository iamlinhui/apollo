package com.ctrip.framework.apollo.spring.boot;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySource;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.ctrip.framework.apollo.spring.property.ApolloAutoUpdateConfigChangeListener;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.foundation.Foundation;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.env.PropertySourcesLoader;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@ConditionalOnClass
public class ApolloApplicationListener implements SpringApplicationRunListener, Ordered {

    private Logger logger = LoggerFactory.getLogger(ApolloApplicationListener.class);

    private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector.getInstance(ConfigPropertySourceFactory.class);

    private final ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);

    private final SpringApplication application;

    private final String[] args;

    private final SimpleApplicationEventMulticaster initialMulticaster;

    private static final AtomicBoolean INITIALIZED_CTX = new AtomicBoolean(false);

    private static final AtomicBoolean INITIALIZED_ENV = new AtomicBoolean(false);

    private static final Splitter NAMESPACE_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

    private static final String DEFAULT_SEARCH_LOCATIONS = "classpath:/,classpath:/config/,file:./,file:./config/";

    private static final String DEFAULT_NAMES = "application,bootstrap,business";

    private static final Multimap<Integer, String> NAMESPACE_NAMES = LinkedHashMultimap.create();

    public static boolean addNamespaces(Collection<String> namespaces, int order) {
        return NAMESPACE_NAMES.putAll(order, namespaces);
    }

    public ApolloApplicationListener(SpringApplication application, String[] args) {
        this.application = application;
        this.args = args;
        this.initialMulticaster = new SimpleApplicationEventMulticaster();
        for (ApplicationListener<?> listener : application.getListeners()) {
            this.initialMulticaster.addApplicationListener(listener);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public void starting() {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment configurableEnvironment) {
        if (NAMESPACE_NAMES.isEmpty()) {
            String namespaces = Foundation.app().getProperty("app.namespace", null);
            if (!StringUtils.isBlank(namespaces)) {
                addNamespaces(NAMESPACE_SPLITTER.splitToList(namespaces), 1);
            }
        }
        if (!NAMESPACE_NAMES.isEmpty() && INITIALIZED_ENV.compareAndSet(false, true)) {
            // 1. 加载本地配置
            loadLocalConfig(configurableEnvironment);
            // 2. 加载远程配置
            loadRemoteConfig(configurableEnvironment);
        }

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext configurableApplicationContext) {
        if (INITIALIZED_CTX.compareAndSet(false, true)) {
            initializeAutoUpdatePropertiesFeature(configurableApplicationContext);
        }
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext configurableApplicationContext) {

    }

    @Override
    public void finished(ConfigurableApplicationContext configurableApplicationContext, Throwable throwable) {

    }

    private void loadRemoteConfig(ConfigurableEnvironment environment) {
        logger.info("ApolloEnvInit initializePropertySources begin");

        if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME)) {
            // already initialized
            return;
        }

        CompositePropertySource composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME);

        // sort by order asc
        Set<Integer> orders = ImmutableSortedSet.copyOf(NAMESPACE_NAMES.keySet());
        for (Integer order : orders) {
            for (String namespace : NAMESPACE_NAMES.get(order)) {
                Config config = ConfigService.getConfig(namespace);
                composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
            }
        }

        // add after the bootstrap property source or to the first
        if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addAfter(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME, composite);
        } else {
            environment.getPropertySources().addLast(composite);
        }

        logger.info("ApolloEnvInit initializePropertySources end");
    }

    private void loadLocalConfig(ConfigurableEnvironment environment) {
        try {

            PropertySourcesLoader propertiesLoader = new PropertySourcesLoader();
            ResourceLoader resourceLoader = new DefaultResourceLoader();

            Set<String> locations = new LinkedHashSet<>(ImmutableSortedSet.copyOf(DEFAULT_SEARCH_LOCATIONS.split(",")));
            Set<String> filenames = new LinkedHashSet<>(ImmutableSortedSet.copyOf(DEFAULT_NAMES.split(",")));
            Set<String> fileExtensions = new LinkedHashSet<>(propertiesLoader.getAllFileExtensions());

            for (String location : locations) {
                for (String filename : filenames) {
                    for (String ext : fileExtensions) {
                        Resource resource = resourceLoader.getResource(location + filename + "." + ext);
                        PropertySource p = propertiesLoader.load(resource, null, filename, null);
                        if (null != p) {
                            environment.getPropertySources().addFirst(p);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("ApolloEnvInit.loadLocalConfig--阿波罗本地文件加载失败!");
        }
    }

    private void initializeAutoUpdatePropertiesFeature(ConfigurableApplicationContext context) {
        if (!configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
            return;
        }

        ApolloAutoUpdateConfigChangeListener apolloAutoUpdateConfigChangeListener = new ApolloAutoUpdateConfigChangeListener(context.getEnvironment());

        List<ConfigPropertySource> configPropertySources = configPropertySourceFactory.getAllConfigPropertySources();
        for (ConfigPropertySource configPropertySource : configPropertySources) {
            configPropertySource.addChangeListener(apolloAutoUpdateConfigChangeListener);
        }
    }

}
