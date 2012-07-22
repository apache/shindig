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
package org.apache.shindig.social.core.oauth;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import org.apache.shindig.auth.OAuthConstants;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.CharsetUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This is largely a copy of OAuthCommandLine with some tweaks for FakeHttpServletRequest
 */
public class FakeOAuthRequest {

  public static enum BodySigning {
    NONE,
    HASH,
    LEGACY
  }

  public static enum OAuthParamLocation {
    AUTH_HEADER,
    POST_BODY,
    URI_QUERY
  }

  public static final String CONSUMER_KEY = "gadget:12345";
  public static final String CONSUMER_SECRET = "secret";
  public static final String REQUESTOR = "requestor12345";

  final String method;
  final String url;
  final String body;
  final String contentType;

  public FakeOAuthRequest(String method, String url, String body, String contentType) {
    this.method = method;
    this.url = url;
    this.body = body;
    this.contentType = contentType;
  }

  public FakeHttpServletRequest sign(String token, OAuthParamLocation paramLocationEnum,
      BodySigning bodySigning)
      throws Exception {
    return sign(CONSUMER_KEY, CONSUMER_SECRET, REQUESTOR, token,
        (token == null) ? null :CONSUMER_SECRET,
        paramLocationEnum, bodySigning);
  }

  public FakeHttpServletRequest sign(String consumerKey, String consumerSecret, String requestor,
      String token, String tokenSecret, OAuthParamLocation paramLocationEnum,
      BodySigning bodySigning)
      throws Exception {
    FakeHttpServletRequest request = new FakeHttpServletRequest(url);

    List<OAuth.Parameter> oauthParams = Lists.newArrayList();
    UriBuilder target = new UriBuilder(Uri.parse(url));
    String query = target.getQuery();
    target.setQuery(null);
    oauthParams.addAll(OAuth.decodeForm(query));

    if (body != null) {
      if (OAuth.isFormEncoded(contentType)) {
        oauthParams.addAll(OAuth.decodeForm(body));
      } else if (bodySigning == BodySigning.LEGACY) {
        oauthParams.add(new OAuth.Parameter(body, ""));
      } else if (bodySigning == BodySigning.HASH) {
        oauthParams.add(
            new OAuth.Parameter(OAuthConstants.OAUTH_BODY_HASH,
                new String(Base64.encodeBase64(DigestUtils.sha(body.getBytes())), "UTF-8")));
      }
    }

    oauthParams.add(new OAuth.Parameter(OAuth.OAUTH_CONSUMER_KEY, consumerKey));
    oauthParams.add(new OAuth.Parameter("xoauth_requestor_id", requestor));

    OAuthConsumer consumer = new OAuthConsumer(null,consumerKey,consumerSecret, null);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    if (!Strings.isNullOrEmpty(token)) {
      accessor.accessToken = token;
      accessor.tokenSecret = tokenSecret;
    }
    OAuthMessage message = accessor.newRequestMessage(method, target.toString(), oauthParams);

    List<Map.Entry<String, String>> entryList = selectOAuthParams(message);

    switch (paramLocationEnum) {
      case AUTH_HEADER:
        request.setHeader("Authorization", getAuthorizationHeader(entryList));
        break;
      case POST_BODY:
        if (!OAuth.isFormEncoded(contentType)) {
          throw new RuntimeException(
              "OAuth param location can only be post_body if post body is of " +
                  "type x-www-form-urlencoded");
        }
        // All message params should be added if oauth params are added to body
        for (Map.Entry<String, String> param : message.getParameters()) {
          request.setParameter(param.getKey(), true, param.getValue());
        }
        String oauthData = OAuth.formEncode(message.getParameters());
        request.setPostData(CharsetUtil.getUtf8Bytes(oauthData));
        break;
      case URI_QUERY:
        request.setQueryString(Uri.parse(OAuth.addParameters(url, entryList)).getQuery());
        break;
    }

    if (body != null && paramLocationEnum != OAuthParamLocation.POST_BODY) {
      request.setContentType(contentType);
      request.setPostData(body, "UTF-8");
      if (contentType.contains(OAuth.FORM_ENCODED)) {
        List<OAuth.Parameter> bodyParams = OAuth.decodeForm(body);
        for (OAuth.Parameter bodyParam : bodyParams) {
          request.setParameter(bodyParam.getKey(), bodyParam.getValue());
        }
      }
    }
    request.setMethod(method);

    return request;
  }

  private static String getAuthorizationHeader(List<Map.Entry<String, String>> oauthParams) {
    StringBuilder result = new StringBuilder("OAuth ");

    boolean first = true;
    for (Map.Entry<String, String> parameter : oauthParams) {
      if (!first) {
        result.append(", ");
      } else {
        first = false;
      }
      result.append(OAuth.percentEncode(parameter.getKey()))
          .append("=\"")
          .append(OAuth.percentEncode(parameter.getValue()))
          .append('"');
    }
    return result.toString();
  }

  private static List<Map.Entry<String, String>> selectOAuthParams(OAuthMessage message)
      throws IOException {
    List<Map.Entry<String, String>> result = Lists.newArrayList();
    for (Map.Entry<String, String> param : message.getParameters()) {
      if (isContainerInjectedParameter(param.getKey())) {
        result.add(param);
      }
    }
    return result;
  }

  private static boolean isContainerInjectedParameter(String key) {
    key = key.toLowerCase();
    return key.startsWith("oauth") || key.startsWith("xoauth") || key.startsWith("opensocial");
  }
}
