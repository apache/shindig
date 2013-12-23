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
package org.apache.shindig.gadgets.templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.w3c.dom.Element;

import com.google.inject.Inject;

/**
 * Factory for template libraries.
 */
public class TemplateLibraryFactory {
  private static final String PARSED_XML_CACHE = "parsedXml";

  private final RequestPipeline pipeline;
  private final Cache<String, Element> parsedXmlCache;

  @Inject
  public TemplateLibraryFactory(RequestPipeline pipeline, CacheProvider cacheProvider) {
    this.pipeline = pipeline;
    // Support null cacheProvider only for testing
    if (cacheProvider == null) {
      this.parsedXmlCache = null;
    } else {
      this.parsedXmlCache = cacheProvider.createCache(PARSED_XML_CACHE);
    }
  }

  public TemplateLibrary loadTemplateLibrary(GadgetContext context, Uri uri) throws GadgetException {
    HttpRequest request = new HttpRequest(uri).setSecurityToken( new AnonymousSecurityToken( "", 0L, context.getUrl().toString()));
    // 5 minute TTL.
    request.setCacheTtl(300);
    request.setIgnoreCache(context.getIgnoreCache());
    HttpResponse response = pipeline.execute(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      int retcode = response.getHttpStatusCode();
      if (retcode == HttpResponse.SC_INTERNAL_SERVER_ERROR) {
        // Convert external "internal error" to gateway error:
        retcode = HttpResponse.SC_BAD_GATEWAY;
      }
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
          "Unable to retrieve template library xml. HTTP error " +
          response.getHttpStatusCode(), retcode);
    }

    String content = response.getResponseAsString();
    try {
      String key = null;
      Element element = null;
      if (!context.getIgnoreCache()) {
        key = HashUtil.checksum(CharsetUtil.getUtf8Bytes(content));
        element = parsedXmlCache.getElement(key);
      }

      if (element == null) {
        // JIRA 1935
        // rewrite the template content to reduce the object number counted by ehcache
        if (!context.getDebug()) {
          BufferedReader reader = new BufferedReader(new StringReader(content));
          StringBuilder sb = new StringBuilder();
          String s;
          try {
            while ((s = reader.readLine()) != null) {
              sb.append(s);
            }
            content = sb.toString();
          } catch (IOException e) {
            // not re-throw exception here
            // If it fails to rewrite the string, just uses the original string for xml parsing
          } finally {
            try {
              reader.close();
            } catch (IOException e) {}
          }
        }

        element = XmlUtil.parse(content);
        if (key != null) {
          parsedXmlCache.addElement(key, element);
        }
      }

      return new XmlTemplateLibrary(uri, element, content);
    } catch (XmlException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e,
          HttpResponse.SC_BAD_REQUEST);
    }
  }

}
