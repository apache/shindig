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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.w3c.dom.Element;

import java.util.concurrent.ExecutorService;

/**
 * Default implementation of a gadget spec factory.
 */
@Singleton
public class DefaultGadgetSpecFactory extends AbstractSpecFactory<GadgetSpec>
    implements GadgetSpecFactory {
  public static final String CACHE_NAME = "gadgetSpecs";
  static final String RAW_GADGETSPEC_XML_PARAM_NAME = "rawxml";
  static final Uri RAW_GADGET_URI = Uri.parse("http://localhost/raw.xml");

  @Inject
  public DefaultGadgetSpecFactory(ExecutorService executor,
                                  RequestPipeline pipeline,
                                  CacheProvider cacheProvider,
                                  @Named("shindig.cache.xml.refreshInterval") long refresh) {
    super(GadgetSpec.class, executor, pipeline, makeCache(cacheProvider), refresh);
  }

  private static Cache<Uri, Object> makeCache(CacheProvider cacheProvider) {
    return cacheProvider.createCache(CACHE_NAME);
  }

  public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
    String rawxml = context.getParameter(RAW_GADGETSPEC_XML_PARAM_NAME);
    if (rawxml != null) {
      // Set URI to a fixed, safe value (localhost), preventing a gadget rendered
      // via raw XML (eg. via POST) to be rendered on a locked domain of any other
      // gadget whose spec is hosted non-locally.
      try {
        return new GadgetSpec(RAW_GADGET_URI, XmlUtil.parse(rawxml), rawxml);
      } catch (XmlException e) {
        throw new SpecParserException(e);
      }
    }

    Uri gadgetUri = context.getUrl();

    Query query = new Query()
        .setSpecUri(gadgetUri)
        .setContainer(context.getContainer())
        .setGadgetUri(gadgetUri)
        .setIgnoreCache(context.getIgnoreCache());
    return super.getSpec(query);
  }

  @Override
  protected GadgetSpec parse(String content, Query query) throws XmlException, GadgetException {
    Element element = XmlUtil.parse(content);
    return new GadgetSpec(query.getSpecUri(), element, content);
  }
}