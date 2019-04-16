package com.ctrip.framework.apollo.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Initialize apollo system properties and inject the Apollo config in Spring Boot bootstrap phase
 *
 * <p>Configuration example:</p>
 * <pre class="code">
 *   # set app.id
 *   app.id = 100004458
 *   # enable apollo bootstrap config and inject 'application' namespace in bootstrap phase
 *   apollo.bootstrap.enabled = true
 * </pre>
 *
 * or
 *
 * <pre class="code">
 *   # set app.id
 *   app.id = 100004458
 *   # enable apollo bootstrap config
 *   apollo.bootstrap.enabled = true
 *   # will inject 'application' and 'FX.apollo' namespaces in bootstrap phase
 *   apollo.bootstrap.namespaces = application,FX.apollo
 * </pre>
 *
 *
 * If you want to load Apollo configurations even before Logging System Initialization Phase,
 *  add
 * <pre class="code">
 *   # set apollo.bootstrap.eagerLoad.enabled
 *   apollo.bootstrap.eagerLoad.enabled = true
 * </pre>
 *
 *  This would be very helpful when your logging configurations is set by Apollo.
 *
 *  for example, you have defined logback-spring.xml in your project, and you want to inject some attributes into logback-spring.xml.
 *
 */
public class ApolloApplicationListener implements SpringApplicationRunListener , Ordered{

  private final SpringApplication application;

  private final String[] args;

  public ApolloApplicationListener(SpringApplication application, String[] args) {
    this.application = application;
    this.args = args;
  }

  @Override
  public void starting() {

  }

  @Override
  public void environmentPrepared(ConfigurableEnvironment configurableEnvironment) {

  }

  @Override
  public void contextPrepared(ConfigurableApplicationContext configurableApplicationContext) {

  }

  @Override
  public void contextLoaded(ConfigurableApplicationContext configurableApplicationContext) {

  }

  @Override
  public void finished(ConfigurableApplicationContext configurableApplicationContext, Throwable throwable) {

  }

  @Override
  public int getOrder() {
    return 0;
  }
}
