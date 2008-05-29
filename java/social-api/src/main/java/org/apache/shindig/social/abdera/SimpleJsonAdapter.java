package org.apache.shindig.social.abdera;

import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.context.BaseResponseContext;
import org.apache.abdera.model.Entry;

/**
 * Classes which wish to handle arbitrary json requests (as gets or posts)
 * should extend this class. Only one method, getResponse, needs to be
 * implemented.
 *
 * This method should return the desired json response in the form
 * of a java object which will be translate with the {@link BeanJsonConverter}.
 *
 * If no response is needed, simply return null.
 *
 * See the {@link org.apache.shindig.social.samplecontainer.SampleContainerRouteManager}
 * and its inner classes for examples of how to handle simple json requests with
 * zero parameters, a parameter provided in a url, or a parameter provided in
 * post data.
 */
public abstract class SimpleJsonAdapter implements CollectionAdapter {
  protected final BeanJsonConverter beanJsonConverter;

  public SimpleJsonAdapter(BeanJsonConverter beanJsonConverter) {
    this.beanJsonConverter = beanJsonConverter;
  }

  /**
   * Handles the abdera request and returns a java pojo which will be ouput
   * as json.
   *
   * @return any java pojo which represents the json response of this
   *     method call. Return null if no response is needed.
   * @param request The abdera request context
   */
  protected abstract Object getResponse(RequestContext request);

  // TODO: We may eventually want to protect some of these requests from gets,
  // and only allow posts. For now though, this is fine.
  public ResponseContext getEntry(RequestContext request) {
    Object responseObject = getResponse(request);

    String content = "{'success' : true}";
    if (responseObject != null) {
      content = beanJsonConverter.convertToJson(responseObject).toString();
    }

    Entry entry = request.getAbdera().getFactory().newEntry();
    entry.setContent(content);
    return new BaseResponseContext<Entry>(entry);
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

  public ResponseContext getFeed(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  public ResponseContext getCategories(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }
}
