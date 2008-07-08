/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social.samplecontainer;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.abdera.atom.ActivityAdapter;
import org.apache.shindig.social.abdera.atom.DataAdapter;
import org.apache.shindig.social.abdera.atom.PersonAdapter;
import org.apache.shindig.social.abdera.json.PersonJsonAdapter;
import org.apache.shindig.social.abdera.json.SimpleJsonAdapter;
import org.apache.shindig.social.abdera.SocialRequestContext;
import org.apache.shindig.social.abdera.SocialRouteManager;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.TargetType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Future;

@Singleton
public class SampleContainerRouteManager extends SocialRouteManager {
  private final DumpStateAdapter dumpStateAdapter;
  private final SetStateAdapter setStateAdapter;
  private final SetEvilnessAdapter setEvilnessAdapter;

  /**
   * Lists all of the restful urls specific to the samplecontainer.
   */
  public static enum SampleContainerUrls {
    /**
     * No parameters need to be passed in. Will return json representing the
     * current state.
     */
    DUMP_STATE("samplecontainer/dumpstate", TargetType.TYPE_ENTRY),

    /**
     * doevil should be "true" or "false" indicating whether the sample
     * container should return data that tries to hack the gadget
     */
    SET_EVILNESS("samplecontainer/setevilness/:doevil", TargetType.TYPE_ENTRY),

    /**
     * This url expects a post parameter called "fileurl".
     */
    SET_STATE("samplecontainer/setstate", TargetType.TYPE_ENTRY);

    private final String routePattern;
    private final TargetType targetType;

    private SampleContainerUrls(String routePattern, TargetType targetType) {
      this.targetType = targetType;
      this.routePattern = routePattern;
    }

    public String getDescription() {
      return toString();
    }

    public String getRoutePattern() {
      return routePattern;
    }

    public TargetType getTargetType() {
      return targetType;
    }
  }

  @Inject
  public SampleContainerRouteManager(PersonJsonAdapter personJsonAdapter,
      PersonAdapter personAtomAdapter, DataAdapter dataAdapter, ActivityAdapter activityAdapter,
      SampleContainerRouteManager.DumpStateAdapter dumpStateAdapter,
      SampleContainerRouteManager.SetStateAdapter setStateAdapter,
      SampleContainerRouteManager.SetEvilnessAdapter setEvilnessAdapter) {
    super(personJsonAdapter, personAtomAdapter, dataAdapter, activityAdapter);
    this.dumpStateAdapter = dumpStateAdapter;
    this.setStateAdapter = setStateAdapter;
    this.setEvilnessAdapter = setEvilnessAdapter;
  }

  public void setRoutes() {
    super.setRoutes();
    this.addRoute(SampleContainerUrls.DUMP_STATE, dumpStateAdapter);
    this.addRoute(SampleContainerUrls.SET_STATE, setStateAdapter);
    this.addRoute(SampleContainerUrls.SET_EVILNESS, setEvilnessAdapter);
  }

  public void addRoute(SampleContainerUrls url,
      CollectionAdapter collectionAdapter) {
    addRoute(url.getDescription(), base + url.getRoutePattern(),
        url.getTargetType(), collectionAdapter);
  }

  // Simple adapters for the sample container

  public static class DumpStateAdapter extends SimpleJsonAdapter {
    private final XmlStateFileFetcher fetcher;

    @Inject
    public DumpStateAdapter(XmlStateFileFetcher fetcher,
        BeanJsonConverter beanJsonConverter) {
      super(beanJsonConverter);
      this.fetcher = fetcher;
    }

    protected Future<ResponseItem<Object>> getEntity(SocialRequestContext request, SecurityToken token) {
      Map<String, Object> state = Maps.newHashMap();
      state.put("people", fetcher.getAllPeople());
      state.put("friendIds", fetcher.getFriendIds());
      state.put("data", fetcher.getAppData());
      state.put("activities", fetcher.getActivities());
      return ImmediateFuture.newInstance(new ResponseItem<Object>(state));
    }

    @Override
    protected Future<ResponseItem> getEntities(SocialRequestContext request, SecurityToken token) {
      return null;
    }
  }

  public static class SetStateAdapter extends SimpleJsonAdapter {
    private final XmlStateFileFetcher fetcher;

    @Inject
    public SetStateAdapter(XmlStateFileFetcher fetcher,
        BeanJsonConverter beanJsonConverter) {
      super(beanJsonConverter);
      this.fetcher = fetcher;
    }

    protected Future<ResponseItem<Object>> getEntity(SocialRequestContext request, SecurityToken token) {
      try {
        String stateFile = getParameter(request, "fileurl");
        fetcher.resetStateFile(new URI(stateFile));
      } catch (URISyntaxException e) {
        sendError(request, "The state file was not a valid url");
      }
      return ImmediateFuture.newInstance(new ResponseItem<Object>(""));
    }

    @Override
    protected Future<ResponseItem> getEntities(SocialRequestContext request, SecurityToken token) {
      // TODO Auto-generated method stub
      return null;
    }
  }

  public static class SetEvilnessAdapter extends SimpleJsonAdapter {
    private final XmlStateFileFetcher fetcher;

    @Inject
    public SetEvilnessAdapter(XmlStateFileFetcher fetcher,
        BeanJsonConverter beanJsonConverter) {
      super(beanJsonConverter);
      this.fetcher = fetcher;
    }

    protected Future<ResponseItem<Object>> getEntity(SocialRequestContext request, SecurityToken token) {
      String doEvil = getParameter(request, "doevil");
      fetcher.setEvilness(Boolean.valueOf(doEvil));
      return ImmediateFuture.newInstance(new ResponseItem<Object>(""));
    }

    @Override
    protected Future<ResponseItem> getEntities(SocialRequestContext request, SecurityToken token) {
      // TODO Auto-generated method stub
      return null;
    }
  }
}
