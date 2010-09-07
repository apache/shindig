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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;

import java.util.List;

/**
 * Response rewriter registry for accel servlet. Encapsulates response rewriters
 * that should be applied for Accel servlet.
 *
 * @since 2.0.0
 */
public class AccelResponseRewriterRegistry extends DefaultResponseRewriterRegistry {
  @Inject
  public AccelResponseRewriterRegistry(@Named("shindig.accelerate.response.rewriters")
                                       List<ResponseRewriter> rewriters,
                                       GadgetHtmlParser htmlParser) {
    super(rewriters, htmlParser);
  }

  /** {@inheritDoc} */
  @Override
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp)
      throws RewritingException {
    HttpResponseBuilder builder = new HttpResponseBuilder(resp);

    if (StringUtils.isEmpty(builder.getContent())) {
      return resp;
    }

    return super.rewriteHttpResponse(req, resp);
  }
}
