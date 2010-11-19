package org.apache.shindig.extras.as;

import java.util.Set;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.JsonContainerConfig;
import org.apache.shindig.extras.as.opensocial.service.ActivityStreamsHandler;
import org.apache.shindig.extras.as.opensocial.spi.ActivityStreamService;
import org.apache.shindig.extras.as.sample.ActivityStreamsJsonDbService;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.conversion.BeanXStreamConverter;
import org.apache.shindig.protocol.conversion.xstream.XStreamConfiguration;
import org.apache.shindig.social.core.util.xstream.XStream081Configuration;
import org.apache.shindig.social.opensocial.service.ActivityHandler;
import org.apache.shindig.social.opensocial.service.AppDataHandler;
import org.apache.shindig.social.opensocial.service.MessageHandler;
import org.apache.shindig.social.opensocial.service.PersonHandler;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/*
 * Note: This class and dataservice > intergration >
 * AbstractActivityStreamsRestfulTests.java are unecessary if ActivityStreams
 * are moved into social-api.  These classes simply register the AS service for
 * testing.  They duplicate their counterparts in social-api other than
 * registering the AS service.
 */
public class ActivityStreamsTestsGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ActivityService.class).to(JsonDbOpensocialService.class);
    bind(AppDataService.class).to(JsonDbOpensocialService.class);
    bind(MessageService.class).to(JsonDbOpensocialService.class);
    bind(PersonService.class).to(JsonDbOpensocialService.class);
    bind(ActivityStreamService.class).to(ActivityStreamsJsonDbService.class);
    
    bind(String.class).annotatedWith(Names.named("shindig.canonical.json.db"))
        .toInstance("sampledata/canonicaldb.json");
    
    bind(XStreamConfiguration.class).to(XStream081Configuration.class);
    bind(BeanConverter.class).annotatedWith(Names.named("shindig.bean.converter.xml")).to(
        BeanXStreamConverter.class);
    bind(BeanConverter.class).annotatedWith(Names.named("shindig.bean.converter.json")).to(
        BeanJsonConverter.class);
    
    bind(new TypeLiteral<Set<Object>>(){}).annotatedWith(
        Names.named("org.apache.shindig.handlers"))
        .toInstance(ImmutableSet.<Object>of(ActivityHandler.class, AppDataHandler.class,
            PersonHandler.class, MessageHandler.class, ActivityStreamsHandler.class));
    
    bindConstant().annotatedWith(Names.named("shindig.containers.default"))
        .to("res://containers/default/container.js");
    bindConstant().annotatedWith(Names.named("shindig.port")).to("8080");
    bindConstant().annotatedWith(Names.named("shindig.host")).to("localhost");
    bind(ContainerConfig.class).to(JsonContainerConfig.class);
    
    bind(Integer.class).annotatedWith(
        Names.named("shindig.cache.lru.default.capacity"))
        .toInstance(10);
  }
}
