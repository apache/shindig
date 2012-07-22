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
package org.apache.shindig.gadgets.oauth;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import org.apache.shindig.auth.OAuthConstants;
import org.apache.shindig.auth.OAuthUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.http.BasicHttpFetcher;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

/**
 *  Run a simple OAuth fetcher to execute a variety of OAuth fetches and output
 *  the result
 *
 *  Arguments
 *  --consumerKey <oauth_consumer_key>
 *  --consumerSecret <oauth_consumer_secret>
 *  --requestorId <xoauth_requestor_id>
 *  --accessToken <oauth_access_token>
 *  --method <GET | POST>
 *  --url <url>
 *  --contentType <contentType>
 *  --postBody <encoded post body>
 *  --postFile <file path of post body contents>
 *  --paramLocation <URI_QUERY | POST_BODY | AUTH_HEADER>
 *  --bodySigning hash|legacy|none
 *  --httpProxy=<http proxy to use for fetching>
 */
public class OAuthCommandLine {

  public static enum BodySigning {
    none,
    hash,
    legacy
  }

  public static void main(String[] argv) throws Exception {
    Map<String, String> params = Maps.newHashMap();
    for (int i = 0; i < argv.length; i+=2) {
      params.put(argv[i], argv[i+1]);
    }
    final String httpProxy = params.get("--httpProxy");
    final String consumerKey = params.get("--consumerKey");
    final String consumerSecret = params.get("--consumerSecret");
    final String xOauthRequestor = params.get("--requestorId");
    final String accessToken = params.get("--accessToken");
    final String tokenSecret = params.get("--tokenSecret");
    final String method = params.get("--method") == null ? "GET" :params.get("--method");
    String url = params.get("--url");
    String contentType = params.get("--contentType");
    String postBody = params.get("--postBody");
    String postFile = params.get("--postFile");
    String paramLocation = params.get("--paramLocation");
    String bodySigning = params.get("--bodySigning");

    HttpRequest request = new HttpRequest(Uri.parse(url));
    if (contentType != null) {
      request.setHeader("Content-Type", contentType);
    } else {
      request.setHeader("Content-Type", OAuth.FORM_ENCODED);
    }
    if (postBody != null) {
      request.setPostBody(postBody.getBytes());
    }
    if (postFile != null) {
      request.setPostBody(IOUtils.toByteArray(new FileInputStream(postFile)));
    }

    OAuthParamLocation paramLocationEnum = OAuthParamLocation.URI_QUERY;
    if (paramLocation != null) {
      paramLocationEnum = OAuthParamLocation.valueOf(paramLocation);
    }

    BodySigning bodySigningEnum = BodySigning.none;
    if (bodySigning != null) {
      bodySigningEnum = BodySigning.valueOf(bodySigning);
    }

    List<OAuth.Parameter> oauthParams = Lists.newArrayList();
    UriBuilder target = new UriBuilder(Uri.parse(url));
    String query = target.getQuery();
    target.setQuery(null);
    oauthParams.addAll(OAuth.decodeForm(query));
    if (OAuth.isFormEncoded(contentType) && request.getPostBodyAsString() != null) {
      oauthParams.addAll(OAuth.decodeForm(request.getPostBodyAsString()));
    } else if (bodySigningEnum == BodySigning.legacy) {
      oauthParams.add(new OAuth.Parameter(request.getPostBodyAsString(), ""));
    } else if (bodySigningEnum == BodySigning.hash) {
      oauthParams.add(
            new OAuth.Parameter(OAuthConstants.OAUTH_BODY_HASH,
                new String(Base64.encodeBase64(
                    DigestUtils.sha(request.getPostBodyAsString().getBytes())), "UTF-8")));
    }

    if (consumerKey != null) {
      oauthParams.add(new OAuth.Parameter(OAuth.OAUTH_CONSUMER_KEY, consumerKey));
    }
    if (xOauthRequestor != null) {
      oauthParams.add(new OAuth.Parameter("xoauth_requestor_id", xOauthRequestor));
    }

    OAuthConsumer consumer = new OAuthConsumer(null, consumerKey, consumerSecret, null);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    accessor.accessToken = accessToken;
    accessor.tokenSecret = tokenSecret;
    OAuthMessage message = accessor.newRequestMessage(method, target.toString(), oauthParams);

    List<Map.Entry<String, String>> entryList = OAuthRequest.selectOAuthParams(message);

    switch (paramLocationEnum) {
      case AUTH_HEADER:
        request.addHeader("Authorization", OAuthRequest.getAuthorizationHeader(entryList));
        break;

      case POST_BODY:
        if (!OAuth.isFormEncoded(contentType)) {
          throw new RuntimeException(
              "OAuth param location can only be post_body if post body if of " +
                  "type x-www-form-urlencoded");
        }
        String oauthData = OAuthUtil.formEncode(message.getParameters());
        request.setPostBody(CharsetUtil.getUtf8Bytes(oauthData));
        break;

      case URI_QUERY:
        request.setUri(Uri.parse(OAuthUtil.addParameters(request.getUri().toString(),
            entryList)));
        break;
    }
    request.setMethod(method);

    HttpFetcher fetcher = new BasicHttpFetcher(httpProxy);
    HttpResponse response = fetcher.fetch(request);

    System.out.println("Request ------------------------------");
    System.out.println(request.toString());
    System.out.println("Response -----------------------------");
    System.out.println(response.toString());
  }
}
