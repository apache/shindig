package org.apache.shindig.social.abdera;

import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.model.DataCollection;
import org.apache.shindig.social.opensocial.model.DataCollection.Data;

import com.google.inject.Inject;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * This adapter translates abdera requests into calls to the DataService.
 * It both fetches and updates Data.
 * TODO: I'm not sure all of this hoisting is right.
 */
public class DataAdapter extends AbstractSocialEntityCollectionAdapter<Data> {
  private static Logger logger = Logger
      .getLogger(DataAdapter.class.getName());

  private DataService dataService;

  @Inject
  public DataAdapter(DataService dataService) {
    this.dataService = dataService;
  }

  @Override
  public Data getEntry(String resourceName,
      RequestContext request) throws ResponseContextException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getId(Data entry) throws ResponseContextException {
    return entry.getPersonId();
  }

  @Override
  public String getName(Data entry)
      throws ResponseContextException {
    return entry.getPersonId();
  }

  @Override
  public String getTitle(Data entry)
      throws ResponseContextException {
    return entry.getPersonId();
  }

  @Override
  public Date getUpdated(Data entry)
      throws ResponseContextException {
    return new Date();
  }

  @Override
  public String getSummary(Data entry)
      throws ResponseContextException {
    return entry.getPersonId();
  }

  /**
   * Query the underlying model for the list of data objects.
   *
   * There is some logic to handle some request dispatching here since this
   * adapter handles the getFeed method for three Data collections:
   * APPDATA_OF_APP_OF_USER, APPDATA_OF_FRIENDS_OF_USER
   *
   * @param request RequestContext
   * @return A List Person entities.
   */
  @Override
  public DataCollection getEntries(RequestContext request)
      throws ResponseContextException {
    String uid = request.getTarget().getParameter("uid");
    List<String> ids = new ArrayList<String>();
    switch (getUrlTemplate(request)) {
      case APPDATA_OF_APP_OF_USER :
        ids.add(uid);
        break;
      case APPDATA_OF_FRIENDS_OF_USER :
        ids = getFriendIds(request, uid);
        break;
      default:
        throw new UnsupportedOperationException(
            "The person adpater was reached with an unsupported url");
    }
    return new DataCollection(dataService.getPersonData(ids, getKeys(request),
        getSecurityToken(request, uid)).getResponse());
  }

  private List<String> getKeys(RequestContext request) {
    String fields = request.getTarget().getParameter("fields");
    if (fields == null) {
      return new ArrayList<String>();
    }
    String[] keyArray = fields.split(",");
    return Arrays.asList(keyArray);
  }

  @Override
  public Data postEntry(String title, IRI id,
      String summary, Date updated,
      List<org.apache.abdera.model.Person> authors, Content content,
      RequestContext request) throws ResponseContextException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putEntry(Data data,
      String title, Date updated, List<org.apache.abdera.model.Person> authors,
      String summary, Content content, RequestContext request)
      throws ResponseContextException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteEntry(String resourceName, RequestContext request)
      throws ResponseContextException {
    throw new UnsupportedOperationException();
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
