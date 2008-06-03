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
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.social.abdera.util.ValidRequestFilter;
import org.apache.shindig.social.abdera.util.ValidRequestFilter.Format;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.IdSpec;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.util.BeanXmlConverter;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.templates.Route;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter;
import org.json.JSONException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * By extending this class it becomes easy to build Collections which are backed
 * by a set of Social entities - such as a person or activity.
 *
 * This base class also has the data connection to the People service and some
 * helper methods for getting social graph information.
 *
 * @param <T> The entity that this is backed by.
 */

public abstract class AbstractSocialEntityCollectionAdapter<T> extends
    AbstractEntityCollectionAdapter<T> {
  private static Logger logger = Logger
      .getLogger(AbstractEntityCollectionAdapter.class.getName());

  protected PeopleService peopleService;
  protected String ID_PREFIX;
  protected Factory factory;
  private BeanXmlConverter beanXmlConverter;
  private BeanJsonConverter beanJsonConverter;
  private SecurityTokenDecoder securityTokenDecoder;

  public AbstractSocialEntityCollectionAdapter() {
    ID_PREFIX = "urn:guid:";
    factory = new Abdera().getFactory();
  }

  /**
   * All the adapters need access to the PeopleService, which has the basic
   * social graph information. Each service adapter will also add an instance
   * of their respective data service.
   * TODO: Also include groups service in base?
   * @param peopleService The people service
   */
  @Inject
  public void setPeopleService(PeopleService peopleService) {
    this.peopleService = peopleService;
  }

  @Inject
  public void setConverters(BeanXmlConverter beanXmlConverter,
      BeanJsonConverter beanJsonConverter) {
    this.beanXmlConverter = beanXmlConverter;
    this.beanJsonConverter = beanJsonConverter;
  }

  @Inject
  public void setSecurityTokenDecoder(SecurityTokenDecoder
      securityTokenDecoder) {
    this.securityTokenDecoder = securityTokenDecoder;
  }

  /**
   * Get the token from the "st" url parameter or throw an exception.
   * @param request Abdera's RequestContext
   * @return SecurityToken
   * @throws SecurityTokenException If the token is invalid
   */
  protected SecurityToken getSecurityToken(RequestContext request)
      throws SecurityTokenException {
    String token = request.getParameter("st");
    if (token == null || token.trim().length() == 0) {
      throw new SecurityTokenException("Missing security token");
    }
    return securityTokenDecoder.createToken(token);
  }

  /**
   * This alternate version of getSecurityToken adds the ability to generate a
   * new security token based on some viewerid supplied.
   *
   * @param request Abdera's RequestContext
   * @param viewerId The viewer ID to fake.
   * @return A call to the parent getSecurityToken which returns a SecurityToken
   */
  protected SecurityToken getSecurityToken(RequestContext request,
      final String viewerId) {
    try {
      return getSecurityToken(request);
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

  /**
   * Gets the IDs of friends for the given user.
   *
   * @param request Abdera's RequestContext
   * @param uid The User ID to get friends for.
   * @return A list of ID strings.
   */
  protected List<String> getFriendIds(RequestContext request, String uid) {
    SecurityToken token = getSecurityToken(request, uid);
    IdSpec idSpec = new IdSpec(null, IdSpec.Type.VIEWER_FRIENDS);
    try {
      return peopleService.getIds(idSpec, token);
    } catch (JSONException e) {
      // TODO: Ignoring this for now. Eventually we can make the service apis
      // fit the restful model better. For now, it is worth some hackiness to
      // keep the interfaces stable.
      return null;
    }
  }

  /**
   * Gets the IDs of connections of the given user.
   *
   * @param request Abdera's RequestContext
   * @param uid The User ID to get connections for.
   * @return A list of ID strings.
   */
  protected List<String> getConnectionIds(RequestContext request, String uid) {
    // TODO: Implement connections. For now, just return friends
    return getFriendIds(request, uid);
   }

  /**
   * @param request RequestContext
   * @param resourceRouteVariable The route variable for the entry. So, for a
   *     route pattern of /:collection/:id, with "id" resourceRouteVariable this
   *     would remove "id" in the generated URL.
   * @return The absolute request URI (includes server name, port, etc) URL for
   *     the collection of the entry.
   */
  public String getFeedIriForEntry(RequestContext request,
      String resourceRouteVariable) {
    Map<String, Object> params = Maps.newHashMap();
    Route theRoute = getRoute(request);
    for (String var: theRoute.getVariables()){
      Object value = request.getTarget().getParameter(var);
      if (!params.containsKey(var) && var != resourceRouteVariable) {
        params.put(var, value);
      }
    }
    String uri = request.urlFor(theRoute.getName(), params);
    return request.getResolvedUri().resolve(uri).toString();
  }

  /**
   * This assumes the target resolver was a RouteManager and returns a Route
   * object. If it does not, it throws a NPE for now. It could also deal with a
   * Regex resolver
   *
   * @param request Abdera's RequestContext
   * @return The Route object that matched the request.
   */
  public static Route getRoute(RequestContext request) {
    Object matcher = request.getTarget().getMatcher();
    if (matcher instanceof Route) {
      return (Route) matcher;
    } else {
      throw new NullPointerException();
    }
  }

  // TODO: We should probably move the static methods here into a helper class
  public static RequestUrlTemplate getUrlTemplate(RequestContext request) {
    String routeName = getRoute(request).getName();
    return RequestUrlTemplate.getValue(routeName);
  }

  @Override
  public Object getContent(T entity, RequestContext request)
      throws ResponseContextException {
    Format format = ValidRequestFilter.getFormatTypeFromRequest(request);
    Content content = factory.newContent();

    switch (format) {
      case ATOM:
        content.setContentType(Content.Type.XML);
        content.setValue(beanXmlConverter.convertToXml(entity));
        break;
      case JSON:
        content.setContentType(Content.Type.TEXT);
        content.setValue(beanJsonConverter.convertToJson(entity).toString());
        break;
    }
    return content;
  }

  public abstract String getSummary(T entity) throws ResponseContextException;

  /**
   * Add the details to an entry.
   *
   * @param request Abdera's RequestContext.
   * @param entry The entry FOM object.
   * @param feedIri The feed IRI that the entry came from.
   * @param entity The object that the entry is based on.
   */
  @Override
  protected String addEntryDetails(RequestContext request, Entry entry,
      IRI feedIri, T entity) throws ResponseContextException {
    addRequiredEntryDetails(request, entry, feedIri, entity);
    addOptionalEntryDetails(request, entry, feedIri, entity);
    return getLink(entity, feedIri, request);
  }

  /**
   * This very similar to the superclass's addEntryDetails but modified to do a
   * minimum of required fields.
   *
   * @param request Abdera's RequestContext.
   * @param entry The entry FOM object.
   * @param feedIri The feed IRI that the entry came from.
   * @param entity The object that the entry is based on.
   * @throws ResponseContextException If the authors can not be fetched
   */
  protected void addRequiredEntryDetails(RequestContext request, Entry entry,
      IRI feedIri, T entity) throws ResponseContextException {
    entry.setId(getId(entity));
    entry.setTitle(getTitle(entity));
    entry.setUpdated(getUpdated(entity));
    entry.setSummary(getSummary(entity));
    List<Person> authors = getAuthors(entity, request);
    if (authors != null) {
      for (Person a : authors) {
        entry.addAuthor(a);
      }
    }
  }

  /**
   * This is a good place for the subclass to do any special processing of the
   * entry element to customize it beyond the basic atom fields like title and
   * author.
   *
   * @param request Abdera's RequestContext.
   * @param entry The entry FOM object.
   * @param feedIri The feed IRI that the entry came from.
   * @param entity The object that the entry is based on.
   * @throws ResponseContextException If some entry data can not be fetched
   */
  protected void addOptionalEntryDetails(RequestContext request, Entry entry,
      IRI feedIri, T entity) throws ResponseContextException {
  }

  /**
   * Create the base feed for the requested collection.
   *
   * TODO: This method needs to be refactored to deal with hoisting and json.
   *
   * @param request Abdera's RequestContext
   */
  @Override
  protected Feed createFeedBase(RequestContext request)
      throws ResponseContextException {
    Factory factory = request.getAbdera().getFactory();
    Feed feed = factory.newFeed();
    String link = getHref(request);
    // TODO: this should create links that are aware of the request format.
    feed.addLink(link, "self", "application/atom+xml", null, null, 0);
    feed.setId(getId(request));
    feed.setTitle(getTitle(request));
    feed.addAuthor(getAuthor(request));
    feed.setUpdated(new Date());
    return feed;
  }

  protected void addEditLinkToEntry(Entry entry) throws Exception {
    if (getEditUriFromEntry(entry) == null) {
      entry.addLink(entry.getId().toString(), "edit");
    }
  }

  protected String getEditUriFromEntry(Entry entry) throws Exception {
    String editUri = null;
    List<Link> editLinks = entry.getLinks("edit");
    if (editLinks != null) {
      for (Link link : editLinks) {
        // if there is more than one edit link, we should not automatically
        // assume that it's always going to point to an Atom document
        // representation.
        if (link.getMimeType() != null) {
          if (link.getMimeType().match("application/atom+xml")) {
            editUri = link.getResolvedHref().toString();
            break;
          }
        } else {
          // edit link with no type attribute is the right one to use
          editUri = link.getResolvedHref().toString();
          break;
        }
      }
    }
   return editUri;
  }
   
  /**
   * Unimplemented HTTP methods
   */

  @Override
  public ResponseContext deleteEntry(RequestContext request) {
    return null;
  }

  @Override
  public ResponseContext putEntry(RequestContext request) {
    return null;
  }

}
