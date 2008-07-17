/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.gadgets.EasyMockTestCase;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.net.URI;
import java.util.Set;

/**
 * Base class for testing content rewriting functionality
 */
public abstract class BaseRewriterTestCase extends EasyMockTestCase {
  static final URI SPEC_URL = URI.create("http://example.org/g.xml");
  protected Set<String> tags;
  protected ContentRewriterFeature contentRewriterFeature;
  protected LinkRewriter defaultRewriter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    tags = Sets.newHashSet("embed", "img", "script", "link");
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithoutRewrite(), ".*", "", "HTTP",
        tags);
    defaultRewriter = new ProxyingLinkRewriter(
      SPEC_URL,
      contentRewriterFeature,
      "http://www.test.com/proxy?url=");
  }

  protected GadgetSpec getSpecWithRewrite(String include, String exclude, String expires,
      Set<String> tags) throws GadgetException {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\">" +
                 "<Optional feature=\"content-rewrite\">\n" +
                 "      <Param name=\"expires\">" + expires + "</Param>\n" +
                 "      <Param name=\"include-urls\">" + include + "</Param>\n" +
                 "      <Param name=\"exclude-urls\">" + exclude + "</Param>\n" +
                 "      <Param name=\"include-tags\">" + StringUtils.join(tags, ",") + "</Param>\n" +
                 "</Optional>" +
                 "</ModulePrefs>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    return new GadgetSpec(SPEC_URL, xml);
  }

  protected GadgetSpec getSpecWithoutRewrite() throws GadgetException {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\">" +
                 "</ModulePrefs>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    return new GadgetSpec(SPEC_URL, xml);
  }
}
