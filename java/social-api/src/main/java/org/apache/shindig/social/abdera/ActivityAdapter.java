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
package org.apache.shindig.social.abdera;

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.model.Activity;

import com.google.inject.Inject;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * This Collection is backed by a set of Activity entities
 */
public class ActivityAdapter extends
    AbstractSocialEntityCollectionAdapter<Activity> {

  private ActivitiesService activitiesService;

  @Inject
  public ActivityAdapter(ActivitiesService activitiesService) {
    this.activitiesService = activitiesService;
  }

  @Override
  public Activity getEntry(String resourceName, RequestContext request)
      throws ResponseContextException {
    String aid = resourceName;
    String uid = request.getTarget().getParameter("uid");
    SecurityToken authToken = getSecurityToken(request, uid);
    return activitiesService.getActivity(uid, aid, authToken).getResponse();
  }

  private SecurityToken getSecurityToken(RequestContext request,
      final String viewerId) {
    try {
      return super.getSecurityToken(request);
    } catch (SecurityTokenException se) {
      // For now, if there's no st param, we'll mock one up.
      try {
        return new BasicSecurityToken("o", viewerId, "a", "d", "u", "m");
      } catch (BlobCrypterException be) {
        be.printStackTrace();
        return null;
      }
    }
  }

  @Override
  public String getId(Activity activityObj) throws ResponseContextException {
    return activityObj.getId();
  }

  // hoisting rule: atom:entry/atom:author/atom:uri aliases "user_id"
  @Override
  public List<Person> getAuthors(Activity activityObj, RequestContext request)
      throws ResponseContextException {
    Person author = factory.newAuthor();
    author.setUri(ID_PREFIX + activityObj.getUserId());
    return Arrays.asList(author);
  }

  @Override
  public String getName(Activity activityObj) throws ResponseContextException {
    return activityObj.getId();
  }

  // hoisting rule: atom:entry/atom:title aliases "title"
  @Override
  public String getTitle(Activity activityObj) throws ResponseContextException {
    return activityObj.getTitle();
  }

  @Override
  public Date getUpdated(Activity activityObj) throws ResponseContextException {
    return activityObj.getUpdated();
  }

  // hoisting rule: atom:entry/atom:summary aliases "body"
  @Override
  public String getSummary(Activity activityObj)
      throws ResponseContextException {
    return activityObj.getBody();
  }

  /**
   * Unimplemented Data methods
   */
  @Override
  public void deleteEntry(String resourceName, RequestContext request)
      throws ResponseContextException {
    // TODO Auto-generated method stub
  }

  @Override
  public Iterable<Activity> getEntries(RequestContext request)
      throws ResponseContextException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Activity postEntry(String title, IRI id, String summary, Date updated,
      List<Person> authors, Content content, RequestContext request)
      throws ResponseContextException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void putEntry(Activity entry, String title, Date updated,
      List<Person> authors, String summary, Content content,
      RequestContext request) throws ResponseContextException {
    // TODO Auto-generated method stub
  }

  /**
   * Collection-level hoisting rules
   */

  /**
   * The collection-level URL. Calls the getFeedIriForEntry and nulls "aid".
   *
   * @param request
   *          RequestContext
   * @return The absolute request URI (includes server name, port, etc) URL
   */
  @Override
  public String getHref(RequestContext request) {
    return getFeedIriForEntry(request, "aid");
  }

  @Override
  public String getId(RequestContext request) {
    // TODO what really to return for the feed ID? Better data will help.
    return getHref(request);
  }

  // hoisting rule: atom:entry/atom:source/atom:title aliases "stream_title"
  // TODO stream_title can't be accessed right here....
  public String getTitle(RequestContext request) {
    return getRoute(request).getName();
  }

  // hoisting rule: atom:entry/atom:author/atom:uri aliases "user_id"
  @Override
  public String getAuthor(RequestContext request)
      throws ResponseContextException {
    return request.getTarget().getParameter("uid");
  }
}
