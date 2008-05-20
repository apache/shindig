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
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.util.BeanXmlConverter;

import com.google.inject.Inject;
import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.templates.Route;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * By extending this class it becomes easy to build Collections which are backed
 * by a set of Social entities - such as a person or activity.
 *
 * @param <T> The entity that this is backed by.
 */

public abstract class AbstractSocialEntityCollectionAdapter<T> extends
    AbstractEntityCollectionAdapter<T> {

  protected String ID_PREFIX;
  protected Factory factory;
  private BeanXmlConverter beanXmlConverter;
  private BeanJsonConverter beanJsonConverter;
  private SecurityTokenDecoder securityTokenDecoder;

  public AbstractSocialEntityCollectionAdapter() {
    ID_PREFIX = "urn:guid:";
    factory = new Abdera().getFactory();
  }

  public enum Format {
    JSON("json"), ATOM("atom");

    private final String displayValue;

    private Format(String displayValue) {
      this.displayValue = displayValue;
    }

    public String getDisplayValue() {
      return displayValue;
    }
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

  protected SecurityToken getSecurityToken(RequestContext request)
      throws SecurityTokenException {
    String token = request.getParameter("st");
    if (token == null || token.trim().length() == 0) {
      throw new SecurityTokenException("Missing security token");
    }
    return securityTokenDecoder.createToken(token);
  }

  /**
   * returns the format (jsoc or atom) from the RequestContext obj created by
   * Abdera from the URL request.
   *
   * @param request the RequestContext obj from Abdera
   * @return the format and default to Format.JSON
   */
  private Format getFormatTypeFromRequest(RequestContext request) {
    String format = request.getTarget().getParameter("format");

    if (format != null && format.equals(Format.ATOM.getDisplayValue())) {
      return Format.ATOM;
    } else {
      return Format.JSON;
    }
  }

  /**
   * @param request RequestContext
   * @param resourceRouteVariable The route variable for the entry. So, for a
   *     route pattern of /:collection/:id, with "id" resourceRouteVariable this
   *     would null out "id" in the generated URL.
   * @return The absolute request URI (includes server name, port, etc) URL for
   *     the collection of the entry.
   */
  public String getFeedIriForEntry(RequestContext request,
      String resourceRouteVariable) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(resourceRouteVariable, null);
    String uri = request.urlFor(getRoute(request).getName(), params);
    return request.getResolvedUri().resolve(uri).toString();
  }

  /**
   * This assumes the target resolver was a RouteManager and returns a Route
   * object. If it does not, it throws a NPE for now. It could also deal with a
   * Regex resolver
   *
   * @param request the request
   * @return The Route object that matched the request.
   */
  public Route getRoute(RequestContext request) {
    Object matcher = request.getTarget().getMatcher();
    if (matcher instanceof Route) {
      return (Route) matcher;
    } else {
      throw new NullPointerException();
    }
  }

  @Override
  public Object getContent(T entity, RequestContext request)
      throws ResponseContextException {
    Format format = getFormatTypeFromRequest(request);
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
   * @param request The request context
   * @param e The entry
   * @param feedIri The feed IRI
   * @param entity The object that the entry is based on.
   */
  @Override
  protected String addEntryDetails(RequestContext request, Entry e,
      IRI feedIri, T entity) throws ResponseContextException {
    String link = getLink(entity, feedIri, request);

    e.addLink(link, "self", "application/atom+xml", null, null, 0);
    e.setId(getId(entity));
    e.setTitle(getTitle(entity));
    e.setUpdated(getUpdated(entity));
    e.setSummary(getSummary(entity));
    List<Person> authors = getAuthors(entity, request);
    if (authors != null) {
      for (Person a : authors) {
        e.addAuthor(a);
      }
    }
    return link;
  }

  /**
   * Unimplemented HTTP methods
   */

  @Override
  public ResponseContext deleteEntry(RequestContext request) {
    return null;
  }

  @Override
  public ResponseContext postEntry(RequestContext request) {
    return null;
  }

  @Override
  public ResponseContext putEntry(RequestContext request) {
    return null;
  }

  @Override
  public ResponseContext getFeed(RequestContext request) {
    return null;
  }
}
