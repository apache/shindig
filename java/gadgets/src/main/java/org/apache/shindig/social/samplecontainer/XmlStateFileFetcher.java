package org.apache.shindig.social.samplecontainer;

import org.w3c.dom.*;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentRequest;
import org.apache.shindig.social.opensocial.model.IdSpec;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Cassandra Doll <doll@google.com>
 */
public class XmlStateFileFetcher {
  private static String defaultStateUrl =
      "http://localhost:8080/gadgets/files/samplecontainer/state-basicfriendlist.xml";

  // TODO: Should use guice here This static fetcher is very gross.
  private static XmlStateFileFetcher fetcher;

  public static XmlStateFileFetcher get() {
    if (fetcher == null) {
      fetcher = new XmlStateFileFetcher();
    }
    return fetcher;
  }

  private URI stateFile;
  private Document document;

  // TODO: This obviously won't work on multiple servers
  // If we care then we should do something about it
  private Map<String, Map<String, String>> allData;
  private Map<IdSpec.Type, List<String>> idMap;
  private Map<String, Person> allPeople;
  private Map<String, List<Activity>> allActivities;

  private XmlStateFileFetcher() {
   try {
      this.stateFile = new URI(defaultStateUrl);
    } catch (URISyntaxException e) {
      throw new RuntimeException(
          "The default state file could not be fetched. ", e);
    }
  }

  public void resetStateFile(URI stateFile) {
    this.stateFile = stateFile;
    this.document = null;
    this.allData = null;
    this.idMap = null;
    this.allPeople = null;
    this.allActivities = null;
  }

  private Document fetchStateDocument() {
    if (document != null) {
      return document;
    }

    // TODO: Eventually get the fetcher and processing options from a
    // config file, just like the GadgetServer
    RemoteContentFetcher fetcher = new BasicRemoteContentFetcher(1024 * 1024);
    RemoteContent xml = fetcher.fetch(new RemoteContentRequest(stateFile));

    InputSource is = new InputSource(new StringReader(
        xml.getResponseAsString()));

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    String errorMessage = "The state file " + stateFile
        + " could not be fetched and parsed.";
    try {
      document = factory.newDocumentBuilder().parse(is);
      return document;
    } catch (SAXException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (IOException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(errorMessage, e);
    }
  }

  public Map<String, Map<String, String>> getAppData() {
    if (allData != null) {
      return allData;
    }

    allData = new HashMap<String, Map<String, String>>();

    Element root = this.fetchStateDocument().getDocumentElement();

    NodeList elements = root.getElementsByTagName("personAppData");
    NodeList personDataNodes = elements.item(0).getChildNodes();

    for (int i = 0; i < personDataNodes.getLength(); i++) {
      Node personDataNode = personDataNodes.item(i);
      NamedNodeMap attributes = personDataNode.getAttributes();
      if (attributes == null) {
        continue;
      }

      String id = attributes.getNamedItem("person").getNodeValue();
      String field = attributes.getNamedItem("field").getNodeValue();
      String value = personDataNode.getTextContent();

      Map<String, String> currentData = allData.get(id);
      if (currentData == null) {
        currentData = new HashMap<String, String>();
        allData.put(id, currentData);
      }
      currentData.put(field, value);
    }

    return allData;
  }

  public void setAppData(String id, String key, String value) {
    if (allData == null) {
      setupPeopleData();
    }

    Map<String, String> personData = allData.get(id);
    if (personData == null) {
      personData = new HashMap<String, String>();
      allData.put(id, personData);
    }

    personData.put(key, value);
  }

  public Map<IdSpec.Type, List<String>> getIdMap() {
    if (idMap == null) {
      setupPeopleData();
    }
    return idMap;
  }

  public Map<String, Person> getAllPeople() {
    if (allPeople == null) {
      setupPeopleData();
    }
    return allPeople;
  }

  private void setupPeopleData() {
    Element root = this.fetchStateDocument().getDocumentElement();

    idMap = new HashMap<IdSpec.Type, List<String>>();
    allPeople = new HashMap<String, Person>();

    // TODO: Eventually the viewer and owner shouldn't be hardcoded. You should
    // be able to visit other allPeople's "profile" pages in the sample container
    setupPeopleInXmlTag(root, "viewer", IdSpec.Type.VIEWER);
    setupPeopleInXmlTag(root, "owner", IdSpec.Type.OWNER);
    setupPeopleInXmlTag(root, "viewerFriends", IdSpec.Type.VIEWER_FRIENDS);
    setupPeopleInXmlTag(root, "ownerFriends", IdSpec.Type.OWNER_FRIENDS);

    // Handle empty people
    if (idMap.get(IdSpec.Type.OWNER).isEmpty()) {
      idMap.put(IdSpec.Type.OWNER, idMap.get(IdSpec.Type.VIEWER));
    }

    if (idMap.get(IdSpec.Type.OWNER_FRIENDS).isEmpty()) {
      idMap.put(IdSpec.Type.OWNER_FRIENDS,
          idMap.get(IdSpec.Type.VIEWER_FRIENDS));
    }
  }

  // Adds all people in the xml tag to the allPeople map.
  // Also puts ids into the idMap under the idType key
  private void setupPeopleInXmlTag(Element root, String tagName,
      IdSpec.Type idType) {
    // TODO: Use the opensource Collections library
    List<String> ids = new ArrayList<String>();

    NodeList elements = root.getElementsByTagName(tagName);
    if (elements == null || elements.item(0) == null) {
      idMap.put(idType, ids);
      return;
    }

    NodeList personNodes = elements.item(0).getChildNodes();

    for (int i = 0; i < personNodes.getLength(); i++) {
      NamedNodeMap attributes = personNodes.item(i).getAttributes();
      if (attributes == null) {
        continue;
      }

      String name = attributes.getNamedItem("name").getNodeValue();
      String id = attributes.getNamedItem("id").getNodeValue();
      Person person = new Person(id, new Name(name));

      Node phoneItem = attributes.getNamedItem("phone");
      if (phoneItem != null) {
        String phone = phoneItem.getNodeValue();
        Phone[] phones = {new Phone(phone, null)};
        person.setPhoneNumbers(phones);
      }

      allPeople.put(id, person);
      ids.add(id);
    }

    idMap.put(idType, ids);
  }

  public Map<String, List<Activity>> getActivities() {
    if (allActivities == null) {
      setupActivities();
    }

    return allActivities;
  }

  private void setupActivities() {
    allActivities = new HashMap<String, List<Activity>>();

    Element root = this.fetchStateDocument().getDocumentElement();
    NodeList activitiesElements = root.getElementsByTagName("activities");

    if (activitiesElements != null && activitiesElements.item(0) != null) {
      NodeList streamItems = activitiesElements.item(0).getChildNodes();

      for (int i = 0; i < streamItems.getLength(); i++) {
        Node streamItem = streamItems.item(i);
        NamedNodeMap streamParams = streamItem.getAttributes();
        String streamTitle = "", userId = "";
        if (streamParams != null) {
          streamTitle = streamParams.getNamedItem("title").getNodeValue();
          userId = streamParams.getNamedItem("userId").getNodeValue();
        }

        createActivities(streamItem, userId, streamTitle);
      }
    }
  }

  private void createActivities(Node streamItem, String userId,
      String streamTitle) {
    NodeList activityItems = streamItem.getChildNodes();
    if (activityItems != null) {
      for (int i = 0; i < activityItems.getLength(); i++) {
        Node activityItem = activityItems.item(i);
        NamedNodeMap activityParams = activityItem.getAttributes();
        if (activityParams == null) {
          continue;
        }

        String title = activityParams.getNamedItem("title").getNodeValue();
        String body = activityParams.getNamedItem("body").getNodeValue();
        String id = activityParams.getNamedItem("id").getNodeValue();

        Activity activity = new Activity(id, userId);
        activity.setStreamTitle(streamTitle);
        activity.setTitle(title);
        activity.setBody(body);
        activity.setMediaItems(getMediaItems(activityItem));

        createActivity(userId, activity);
      }
    }
  }

  private List<MediaItem> getMediaItems(Node activityItem) {
    List<MediaItem> media = new ArrayList<MediaItem>();

    NodeList mediaItems = activityItem.getChildNodes();
    if (mediaItems != null) {
      for (int i = 0; i < mediaItems.getLength(); i++) {
        NamedNodeMap mediaParams = mediaItems.item(i).getAttributes();
        if (mediaParams != null) {
          String typeString = mediaParams.getNamedItem("type").getNodeValue();
          String mimeType = mediaParams.getNamedItem("mimeType").getNodeValue();
          String url = mediaParams.getNamedItem("url").getNodeValue();

          media.add(new MediaItem(mimeType,
              MediaItem.Type.valueOf(typeString), url));
        }
      }
    }

    return media;
  }

  public void createActivity(String userId, Activity activity) {
    if (allActivities == null) {
      setupActivities();
    }

    if (allActivities.get(userId) == null) {
      allActivities.put(userId, new ArrayList<Activity>());
    }
    allActivities.get(userId).add(activity);
  }
}
