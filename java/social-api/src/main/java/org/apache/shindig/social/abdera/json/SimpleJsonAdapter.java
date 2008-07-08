package org.apache.shindig.social.abdera.json;

import com.google.inject.Inject;

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.abdera.RawResponseContext;
import org.apache.shindig.social.abdera.SocialRequestContext;
import org.apache.shindig.social.dataservice.PersonService;
import org.apache.shindig.social.dataservice.RestfulCollection;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import org.apache.abdera.model.AtomDate;
import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter;
import org.apache.abdera.util.EntityTag;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

/**
 * Classes which wish to handle arbitrary json requests (as gets or posts) should extend this class.
 * Only one method, getResponse, needs to be implemented.
 *
 * This method should return the desired json response in the form of a java object which will be
 * translate with the {@link BeanJsonConverter}.
 *
 * If no response is needed, simply return null.
 *
 * See the {@link org.apache.shindig.social.samplecontainer.SampleContainerRouteManager} and its
 * inner classes for examples of how to handle simple json requests with zero parameters, a
 * parameter provided in a url, or a parameter provided in post data.
 * @param <T>
 */
public abstract class SimpleJsonAdapter<T> implements CollectionAdapter {

  private static final Logger logger = Logger
      .getLogger(AbstractEntityCollectionAdapter.class.getName());

  protected final BeanJsonConverter beanJsonConverter;
  protected SecurityTokenDecoder securityTokenDecoder;
  protected PersonService personService;

  @Inject
  public SimpleJsonAdapter(BeanJsonConverter beanJsonConverter) {
    this.beanJsonConverter = beanJsonConverter;
  }

  /**
   * All the adapters need access to the PeopleService, which has the basic
   * social graph information. Each service adapter will also add an instance
   * of their respective data service.
   * TODO: Also include groups service in base?
   * @param personService The people service
   */
  @Inject
  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  @Inject
  public void setSecurityTokenDecoder(SecurityTokenDecoder
      securityTokenDecoder) {
    this.securityTokenDecoder = securityTokenDecoder;
  }

  /**
   * Used during getEntry and returns a java pojo which will be ouput as json.
   *
   * @return any java pojo which represents the json response of this method call. Return null if no
   *         response is needed.
   * @param request The abdera request context
   */
  protected abstract Future<ResponseItem<T>> getEntity(SocialRequestContext request, SecurityToken token);

  /**
   * Used during getFeed and returns a list of java pojos which will be ouput as json.
   * 
   * @return any java pojo which represents the json response of this method call. Return null if no
   *         response is needed.
   * @param request The abdera request context
   */
  protected abstract Future<ResponseItem<RestfulCollection<T>>> getEntities(SocialRequestContext request,
      SecurityToken token);

  public ResponseContext getEntry(RequestContext request) {
    SecurityToken token = getSecurityToken(request);
    if (token == null){
      return ProviderHelper.badrequest(request, "Missing security token");
    }
    Future<? extends ResponseItem> itemFuture = getEntity(new SocialRequestContext(request), token);

    // TODO Need to get updated from the entity!!! (probably through the ResponseItem)
    // TODO Also need some have some access to the id of the object
    // TODO Without these, you cannot set the etag and last-modified headers
    Date updated = new Date(); // entities.getUpdated();
    String guid = null; // entities.getId();;
    return jsonResponse(request, itemFuture, guid, updated);
  }

  public ResponseContext getFeed(RequestContext request) {
    SecurityToken token = getSecurityToken(request);
    if (token == null){
      return ProviderHelper.badrequest(request, "Missing security token");
    }
    Future<? extends ResponseItem> itemFuture = getEntities(
        new SocialRequestContext(request), token);
    // TODO Need to get updated from the RestfulCollection!!! (probably through the ResponseItem)
    // TODO Also need some have some access to the id of the RestfulCollection
    // TODO Without these, you cannot set the etag and last-modified headers
    Date updated = new Date(); // entities.getUpdated();
    String guid = null; // entities.getId();;
    return jsonResponse(request, itemFuture, guid, updated);
  }

  private RawResponseContext jsonResponse(RequestContext request,
      Future<? extends ResponseItem> item, String guid,
      Date updated) {
    try {
      Object content = item.get().getResponse();
      String output = null;
      if (content != null) {
        output = beanJsonConverter.convertToJson(content).toString();
      }
      RawResponseContext response = new RawResponseContext(output, 200);
      response.setContentType("application/json");
      response.setLastModified(updated);
      response.setEntityTag(EntityTag.generate(guid, AtomDate.format(updated)));
      return response;
    } catch (UnsupportedEncodingException e) {
      logger.log(Level.WARNING, "Encoding error", e);
      return (RawResponseContext) ProviderHelper.servererror(request, e);
    } catch (ExecutionException e) {
      logger.log(Level.WARNING, "Execution error", e);
      return (RawResponseContext) ProviderHelper.servererror(request, e);
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Interrputed", e);
      return (RawResponseContext) ProviderHelper.servererror(request, e);
    }
  }

  protected String getParameter(RequestContext request, String name) {
    return request.getTarget().getParameter(name);
  }

  protected void sendError(RequestContext request, String message) {
    ProviderHelper.badrequest(request, message);
  }

  // This method is called when abdera receives a post it doesn't understand in
  // xml. All post parameters are still in the same place as the get call, so
  // we can just dispatch here and the underlying json handlers won't know the
  // difference.
  public ResponseContext extensionRequest(RequestContext requestContext) {
    return getEntry(requestContext);
  }

  // Unimplemented methods. Simple json classes only handle gets and posts as
  // they are not associated with any particular pojos. If you wish to implement
  // these methods use the AbstractSocialEntityCollectionAdapter class.

  public ResponseContext postEntry(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  public ResponseContext deleteEntry(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  public ResponseContext headEntry(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  public ResponseContext optionsEntry(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  public ResponseContext putEntry(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }


  public ResponseContext getCategories(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the token from the "st" url parameter or throw an exception.
   * @param request Abdera's RequestContext
   * @return SecurityToken
   * @throws SecurityTokenException If the token is invalid
   */
  /**
   * Reads the gadget security token out of an {@link HttpServletRequest},
   * making sure to return null if there are problems.
   */
  public SecurityToken getSecurityToken(RequestContext request) {
    String token = request.getParameter(SocialRequestContext.SECURITY_TOKEN_PARAM);

    if (token == null || token.trim().length() == 0) {
      return null;
    }

    try {
      Map<String, String> params =
          Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME,
              token);
      return securityTokenDecoder.createToken(params);
    } catch (SecurityTokenException e) {
      String message = new StringBuilder()
          .append("found security token, but couldn't decode it ")
          .append("(treating it as not present). token is: ")
          .append(token)
          .toString();
      logger.warning(message);
      return null;
    }
  }

  /**
   * This alternate version of getSecurityToken adds the ability to generate a
   * new security token based on some viewerid supplied.
   *
   * @param request Abdera's RequestContext
   * @param viewerId The viewer ID to fake.
   * @return A call to the parent getSecurityToken which returns a SecurityToken
   */
  public SecurityToken getSecurityToken(RequestContext request,
      final String viewerId) {
    SecurityToken token = getSecurityToken(request);
    if (token == null) {
      try {
        return new BasicSecurityToken("o", viewerId, "a", "d", "u", "m");
      } catch (BlobCrypterException be) {
        be.printStackTrace();
        return null;
      }
    }
     return token;
  }
}
