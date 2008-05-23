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
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.Person;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * This Collection is backed by a set of Person entities
 * The Adapter needs People and Groups.
 * The PeopleService comes from the base class. Groups is unimplemented.
 */
public class PersonAdapter extends
    AbstractSocialEntityCollectionAdapter<Person> {
  private static Logger logger = Logger
      .getLogger(PersonAdapter.class.getName());

  /**
   * Query the underlying model for an Person object.
   *
   * @param resourceName An id string that identifies a Person.
   * @param request Abdera's RequestContext.
   * @return An Person entity.
   */
  @Override
  public Person getEntry(String resourceName, RequestContext request)
      throws ResponseContextException {
    String uid = request.getTarget().getParameter("uid");
    // Get a token assuming the {uid} is the viewerid.
    // TODO: Replace hardcoded token viewerid with a SubjectResolver.
    SecurityToken authToken = getSecurityToken(request, uid);
    return peopleService.getPerson(resourceName, authToken).getResponse();
  }

  /**
   * Get's the name of the specific resource requested.
   * There is some logic to handle some request parsing here since this
   * adapter handles the getEntry method for two Person entries:
   * PROFILE_OF_CONNECTION_OF_USER and PROFILE_OF_USER
   */
  @Override
  protected String getResourceName(RequestContext request) {
    switch (getUrlTemplate(request)) {
      case PROFILE_OF_CONNECTION_OF_USER:
        // TODO: Improve the service apis so we can get rid of relational code.
        for (String cid : getConnectionIds(request, request.getTarget()
            .getParameter("uid"))) {
          if (cid.equals(request.getTarget().getParameter("pid"))) {
            return cid;
          }
        }
        return null;
      case PROFILE_OF_USER:
        return request.getTarget().getParameter("uid");
      default:
        // TODO: Clean this code up so we don't need this check
        throw new UnsupportedOperationException(
            "The person adpater was reached with an unsupported url");
    }
  }

  /**
   * atom:entry/atom:id aliases the "id" field. In the Atom format, it is
   * translated into the required URI data type by prepending "urn:guid:" to the
   * OpenSocial ID string.
   */
  @Override
  public String getId(Person personObj) throws ResponseContextException {
    return ID_PREFIX + personObj.getId();
  }

  // hoisting rule: atom:entry/atom:author/atom:uri aliases ?
  @Override
  public List<org.apache.abdera.model.Person> getAuthors(Person personObj,
      RequestContext request) throws ResponseContextException {
    org.apache.abdera.model.Person author = factory.newAuthor();
    author.setUri(ID_PREFIX + personObj.getId());
    return Arrays.asList(author);
  }

  /**
   * Get the name of the entry resource (used to construct links)
   */
  @Override
  public String getName(Person personObj) throws ResponseContextException {
    return personObj.getId();
  }

  // hoisting rule: atom:entry/atom:title aliases ?
  @Override
  public String getTitle(Person personObj) throws ResponseContextException {
    return personObj.getName().getUnstructured();
  }

  @Override
  public Date getUpdated(Person personObj) throws ResponseContextException {
    return personObj.getUpdated();
  }

  // hoisting rule: atom:entry/atom:summary aliases ?
  @Override
  public String getSummary(Person personObj) throws ResponseContextException {
    return null;
  }

  // hoisting rule: atom:entry/atom:published aliases ?
  public Long getPublished(Person personObj)
      throws ResponseContextException {
    // TODO: Add published element to the entry object.
    // TODO: Switch based on output format from RFC date to epoch-based.
    // POSTED_TIME is seconds since the epoch.
    return null;
  }

  /**
   * This is where some person entry format customization happens.
   *
   * @param request Abdera's RequestContext.
   * @param entry The entry FOM object.
   * @param feedIri The feed IRI that the entry came from.
   * @param personObj The object that the entry is based on.
   * @throws ResponseContextException
   */
  @Override
  protected void addOptionalEntryDetails(RequestContext request, Entry entry,
      IRI feedIri, Person personObj) throws ResponseContextException {
    String link = getLink(personObj, feedIri, request);
    // TODO: this should create links that are aware of the request format.
    entry.addLink(link, "self", "application/atom+xml", null, null, 0);

    // TODO:
    // atom:entry/atom:published aliases ?
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
   * Query the underlying model for the list person objects.
   *
   * There is some logic to handle some request dispatching here since this
   * adapter handles the getFeed method for three Person collections:
   * PROFILES_OF_CONNECTIONS_OF_USER, PROFILES_OF_FRIENDS_OF_USER and
   * PROFILES_IN_GROUP_OF_USER
   *
   * @param request RequestContext
   * @return A List Person entities.
   */
  @Override
  public Iterable<Person> getEntries(RequestContext request)
      throws ResponseContextException {
    String uid = request.getTarget().getParameter("uid");
    List<String> ids;
    switch (getUrlTemplate(request)) {
      case PROFILES_OF_CONNECTIONS_OF_USER :
        ids = getConnectionIds(request, uid);
        break;
      case PROFILES_OF_FRIENDS_OF_USER :
        // TODO: Change activities service to handle the friend lookup itself
        ids = getFriendIds(request, uid);
        break;
      case PROFILES_IN_GROUP_OF_USER :
        // TODO: add something like ids = getGroupIds(request, gid);
        // For now, this just returns the friends.
        ids = getFriendIds(request, uid);
        break;
      default:
        // TODO: Clean this code up so we don't need this check
        throw new UnsupportedOperationException(
            "The person adpater was reached with an unsupported url");
    }
    // Get a token assuming the {uid} is the viewerid.
    // TODO: Replace hardcoded token viewerid with a SubjectResolver.
    SecurityToken authToken = getSecurityToken(request, uid);
    return peopleService.getPeople(ids, PeopleService.SortOrder.name, null, 0,
        100, null, authToken).getResponse().getItems();
  }

  @Override
  public Person postEntry(String title, IRI id, String summary, Date updated,
      List<org.apache.abdera.model.Person> authors, Content content,
      RequestContext request) throws ResponseContextException {
    // TODO: Implement
    return null;
  }

  @Override
  public void putEntry(Person personObj, String title, Date updated,
      List<org.apache.abdera.model.Person> authors, String summary,
      Content content, RequestContext request) throws ResponseContextException {
    // TODO: Implement
  }

  /**
   * Collection-level hoisting rules
   */

  /**
   * The collection-level URL. Calls the getFeedIriForEntry and nulls "pid".
   *
   * @param request RequestContext
   * @return The absolute request URI (includes server name, port, etc) URL
   */
  @Override
  public String getHref(RequestContext request) {
    return getFeedIriForEntry(request, "pid");
  }

  @Override
  public String getId(RequestContext request) {
    // TODO: what really to return for the feed ID? Better data will help.
    return getHref(request);
  }

  public String getTitle(RequestContext request) {
    return getRoute(request).getName();
  }

  // hoisting rule: atom:entry/atom:author/atom:uri aliases ?
  @Override
  public String getAuthor(RequestContext request)
      throws ResponseContextException {
    return request.getTarget().getParameter("uid");
  }

}
