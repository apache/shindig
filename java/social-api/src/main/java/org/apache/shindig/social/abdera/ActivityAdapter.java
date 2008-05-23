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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.model.Activity;

import com.google.inject.Inject;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * This Collection is backed by a set of Activity entities
 */
public class ActivityAdapter extends
    AbstractSocialEntityCollectionAdapter<Activity> {
  private static Logger logger = Logger
      .getLogger(ActivityAdapter.class.getName());

  private ActivitiesService activitiesService;

  /**
   * The Adapter needs Activities, People and Groups.
   * The PeopleService comes from the base class. Groups is unimplemented.
   * @param activitiesService The activities service
   */
  @Inject
  public ActivityAdapter(ActivitiesService activitiesService) {
    this.activitiesService = activitiesService;
  }

  /**
   * Query the underlying model for an Activity object.
   *
   * @param request RequestContext
   * @return An Activity entity.
   */
  @Override
  public Activity getEntry(String resourceName, RequestContext request)
      throws ResponseContextException {
    String aid = resourceName;
    String uid = request.getTarget().getParameter("uid");
    SecurityToken authToken = getSecurityToken(request, uid);
    return activitiesService.getActivity(uid, aid, authToken).getResponse();
  }

  /**
   * atom:entry/atom:id aliases the "id" field. In the Atom format, it is
   * translated into the required URI data type by prepending "urn:guid:" to the
   * OpenSocial ID string.
   */
  @Override
  public String getId(Activity activityObj) throws ResponseContextException {
    return ID_PREFIX + activityObj.getId();
  }

  // hoisting rule: atom:entry/atom:author/atom:uri aliases "user_id"
  @Override
  public List<Person> getAuthors(Activity activityObj, RequestContext request)
      throws ResponseContextException {
    Person author = factory.newAuthor();
    author.setUri(ID_PREFIX + activityObj.getUserId());
    return Arrays.asList(author);
  }

  /**
   * Get the name of the entry resource (used to construct links)
   */
  @Override
  public String getName(Activity activityObj) throws ResponseContextException {
    return activityObj.getId();
  }

  // hoisting rule: atom:entry/atom:title aliases "title"
  @Override
  public String getTitle(Activity activityObj) throws ResponseContextException {
    return activityObj.getTitle();
  }

  // hoisting rule: atom:entry/atom:updated aliases POSTED_TIME for Activity
  // or the generation time if no better information is available.
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

  // hoisting rule: atom:entry/atom:published aliases POSTED_TIME
  public Long getPublished(Activity activityObj)
      throws ResponseContextException {
    // TODO: Add published element to the entry object.
    // TODO: Switch based on output format from RFC date to epoch-based.
    // POSTED_TIME is seconds since the epoch.
    return activityObj.getPostedTime();
  }

  /**
   * This is where some activity entry format customization happens.
   *
   * @param request Abdera's RequestContext.
   * @param entry The entry FOM object.
   * @param feedIri The feed IRI that the entry came from.
   * @param activityObj The object that the entry is based on.
   * @throws ResponseContextException
   */
  protected void addOptionalEntryDetails(RequestContext request, Entry entry,
      IRI feedIri, Activity activityObj) throws ResponseContextException {
    String link = getLink(activityObj, feedIri, request);
    // TODO: this should create links that are aware of the request format.
    entry.addLink(link, "self", "application/atom+xml", null, null, 0);

    // TODO:
    // atom:entry/atom:generator/atom:uri aliases "app_id"
    // atom:entry/atom:published aliases POSTED_TIME
  }

  /**
   * Unimplemented Data methods
   */
  @Override
  public void deleteEntry(String resourceName, RequestContext request)
      throws ResponseContextException {
    // TODO: Auto-generated method stub
  }

  /**
   * Query the underlying model for the list activity objects.
   *
   * There is some logic to handle some request dispatching here since this
   * adapter handles the getFeed method for three Activity collections:
   * ACTIVITIES_OF_USER, ACTIVITIES_OF_FRIENDS_OF_USER and
   * ACTIVITIES_OF_GROUP_OF_USER
   *
   * @param request RequestContext
   * @return A List Activity entities.
   */
  @Override
  public Iterable<Activity> getEntries(RequestContext request)
      throws ResponseContextException {
    String uid = request.getTarget().getParameter("uid");
    List<String> ids = new ArrayList<String>();
    switch (getUrlTemplate(request)) {
      case ACTIVITIES_OF_USER :
        ids.add(uid);
        break;
      case ACTIVITIES_OF_FRIENDS_OF_USER :
        // TODO: Change activities service to handle the friend lookup itself
        ids = getFriendIds(request, uid);
        break;
      case ACTIVITIES_OF_GROUP_OF_USER :
        // TODO: add something like ids = getGroupIds(request, gid);
        String gid = request.getTarget().getParameter("gid");
        break;
      default:
        // TODO: Clean this code up so we don't need this check
        throw new UnsupportedOperationException(
            "The activity adpater was reached with an unsupported url");
    }

    SecurityToken authToken = getSecurityToken(request, uid);
    return activitiesService.getActivities(ids, authToken).getResponse();
  }

  @Override
  public Activity postEntry(String title, IRI id, String summary, Date updated,
      List<Person> authors, Content content, RequestContext request)
      throws ResponseContextException {
    // TODO: Implement
    return null;
  }

  @Override
  public void putEntry(Activity entry, String title, Date updated,
      List<Person> authors, String summary, Content content,
      RequestContext request) throws ResponseContextException {
    // TODO: Implement
  }

  /**
   * Collection-level hoisting rules
   */

  /**
   * The collection-level URL. Calls the getFeedIriForEntry and nulls "aid".
   *
   * @param request RequestContext
   * @return The absolute request URI (includes server name, port, etc) URL
   */
  @Override
  public String getHref(RequestContext request) {
    return getFeedIriForEntry(request, "aid");
  }

  @Override
  public String getId(RequestContext request) {
    // TODO: what really to return for the feed ID? Better data will help.
    return getHref(request);
  }

  // hoisting rule: atom:entry/atom:source/atom:link@rel="self" aliases
  // "stream_url"
  // TODO: "stream_url"

  // hoisting rule: atom:entry/atom:source/atom:title aliases "stream_title"
  // TODO: stream_title can't be accessed right here....
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
