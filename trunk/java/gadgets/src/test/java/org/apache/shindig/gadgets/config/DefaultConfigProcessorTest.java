/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.config;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.easymock.EasyMock;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DefaultConfigProcessorTest {
  private static final String CONFIG_FEATURE = "config-feature";
  private static final List<String> CONFIG_FEATURES = Lists.newArrayList(CONFIG_FEATURE);
  private static final Map<String, Object> CONFIG_FEATURE_MAP = ImmutableMap.<String, Object>of("key1", "val1", "key2", "val2");
  private static final String NOCONFIG_FEATURE = "noconfig-feature";
  private static final String CONTAINER = "container";
  private static final String HOST = "host";
  private static final Gadget GADGET = new Gadget();

  private ContainerConfig config;

  @Before
  public void setUp() {
    config = createMock(ContainerConfig.class);
    expect(config.getMap(CONTAINER, DefaultConfigProcessor.GADGETS_FEATURES_KEY)).andReturn(CONFIG_FEATURE_MAP);
    replay(config);
  }

  @Test
  public void testGlobalConfig() {
    ConfigContributor contrib = mockContrib(HOST);
    List<ConfigContributor> globalContrib = Lists.newArrayList(contrib);
    ConfigContributor noContrib = mockContrib((String)null);
    Map<String, ConfigContributor> featureContrib = ImmutableMap.of(NOCONFIG_FEATURE, noContrib);
    DefaultConfigProcessor processor = new DefaultConfigProcessor(featureContrib, config);
    processor.setGlobalContributors(globalContrib);
    processor.getConfig(CONTAINER, CONFIG_FEATURES, HOST, null);
    verify(config, contrib, noContrib);
  }

  @Test
  public void testFeatureConfigHost() {
    ConfigContributor contrib = mockContrib(HOST);
    List<ConfigContributor> globalContrib = Lists.newArrayList();
    ConfigContributor noContrib = mockContrib((String)null);
    Map<String, ConfigContributor> featureContrib = ImmutableMap.of(CONFIG_FEATURE, contrib,
        NOCONFIG_FEATURE, noContrib);
    DefaultConfigProcessor processor = new DefaultConfigProcessor(featureContrib, config);
    processor.setGlobalContributors(globalContrib);
    processor.getConfig(CONTAINER, CONFIG_FEATURES, HOST, null);
    verify(config, contrib, noContrib);
  }

  @Test
  public void testFeatureConfigGadget() {
    ConfigContributor contrib = mockContrib(GADGET);
    List<ConfigContributor> globalContrib = Lists.newArrayList();
    ConfigContributor noContrib = mockContrib((Gadget)null);
    Map<String, ConfigContributor> featureContrib = ImmutableMap.of(CONFIG_FEATURE, contrib,
        NOCONFIG_FEATURE, noContrib);
    DefaultConfigProcessor processor = new DefaultConfigProcessor(featureContrib, config);
    processor.setGlobalContributors(globalContrib);
    processor.getConfig(CONTAINER, CONFIG_FEATURES, null, GADGET);
    verify(config, contrib, noContrib);
  }

  @SuppressWarnings("unchecked")
  private ConfigContributor mockContrib(String host) {
    ConfigContributor contrib = EasyMock.createMock(ConfigContributor.class);
    createMock(ConfigContributor.class);
    if (host != null) {
      contrib.contribute((Map<String, Object>) notNull(), eq(CONTAINER), eq(host));
      expectLastCall();
    }
    replay(contrib);
    return contrib;
  }

  @SuppressWarnings("unchecked")
  private ConfigContributor mockContrib(Gadget gadget) {
    ConfigContributor contrib = EasyMock.createMock(ConfigContributor.class);
    createMock(ConfigContributor.class);
    if (gadget != null) {
      contrib.contribute((Map<String, Object>) notNull(), eq(gadget));
      expectLastCall();
    }
    replay(contrib);
    return contrib;
  }
}
