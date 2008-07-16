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
package org.apache.shindig.social.samplecontainer;

import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.dataservice.DataRequestHandler;
import org.apache.shindig.social.dataservice.RequestItem;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Future;

// TODO(doll): This class won't be needed anymore once we switch to the canonical data set
public class SampleContainerHandler extends DataRequestHandler {
  private final XmlStateFileFetcher fetcher;
  private static final String POST_PATH = "/samplecontainer/{type}/{doevil}";

  @Inject
  public SampleContainerHandler(XmlStateFileFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /**
   * We don't support any delete methods right now
   */
  protected Future<? extends ResponseItem> handleDelete(RequestItem request) {
    throw new UnsupportedOperationException();
  }

  /**
   * We don't distinguish between put and post for these urls
   */
  protected Future<? extends ResponseItem> handlePut(RequestItem request) {
    return handlePost(request);
  }

  public static class SetStateInput {
    public String fileUrl;

    public String getFileUrl() {
      return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
      this.fileUrl = fileUrl;
    }
  }

  /**
   * Handles /samplecontainer/setstate and /samplecontainer/setevilness/{doevil}
   * TODO(doll): These urls aren't very resty. Consider changing the samplecontainer.html calls
   * post.
   */
  protected Future<? extends ResponseItem> handlePost(RequestItem request) {
    ResponseItem<Object> response = new ResponseItem<Object>("");

    request.parseUrlWithTemplate(POST_PATH);
    String type = request.getParameters().get("type");
    if (type.equals("setstate")) {
      try {
        String stateFile = request.getParameters().get("fileurl");
        fetcher.resetStateFile(new URI(stateFile));
      } catch (URISyntaxException e) {
        response = new ResponseItem<Object>(ResponseError.BAD_REQUEST,
            "The state file was not a valid url", null);
      }
    } else if (type.equals("setevilness")) {
      String doEvil = request.getParameters().get("doevil");
      fetcher.setEvilness(Boolean.valueOf(doEvil));
    }

    return ImmediateFuture.newInstance(response);
  }

  /**
   * Handles /samplecontainer/dumpstate
   */
  protected Future<? extends ResponseItem> handleGet(RequestItem request) {
    Map<String, Object> state = Maps.newHashMap();
    state.put("people", fetcher.getAllPeople());
    state.put("friendIds", fetcher.getFriendIds());
    state.put("data", fetcher.getAppData());
    state.put("activities", fetcher.getActivities());
    return ImmediateFuture.newInstance(new ResponseItem<Object>(state));
  }
}