package org.apache.shindig.social.samplecontainer;

import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContentRequest;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.Enum;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cassandra Doll <doll@google.com>
 */
public class XmlStateFileFetcher {
  private static final String DEFAULT_STATE_URL
      = "http://localhost:8080/gadgets/files/samplecontainer/"
      + "state-basicfriendlist.xml";

  // Evil javascript strings
  private static final String REDEFINE_NEW_DATA_REQUEST
      = "opensocial.newDataRequest = "
      + "function() { alert('Ha! I attacked you!')}; ";

  private static final String MAKE_PAGE_RED
      = "document.body.style.backgroundColor = 'red'; ";

  private static final String SCRIPT_PREFIX = "<div onMouseOver=\""
      + REDEFINE_NEW_DATA_REQUEST + MAKE_PAGE_RED + "\">";

  private static final String SCRIPT_SUFFIX = "</div>";


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
  private boolean doEvil = false;

  // TODO: This obviously won't work on multiple servers
  // If we care then we should do something about it
  private Map<String, Map<String, String>> allData;
  private Map<String, List<String>> friendIdMap;
  private Map<String, Person> allPeople;
  private Map<String, List<Activity>> allActivities;

  private XmlStateFileFetcher() {
   try {
      stateFile = new URI(DEFAULT_STATE_URL);
    } catch (URISyntaxException e) {
      throw new RuntimeException(
          "The default state file could not be fetched. ", e);
    }
  }

  public void resetStateFile(URI stateFile) {
    this.stateFile = stateFile;
    document = null;
    allData = null;
    friendIdMap = null;
    allPeople = null;
    allActivities = null;
  }

  public void setEvilness(boolean doEvil) {
    this.doEvil = doEvil;
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

  private String turnEvil(String originalString) {
    if (doEvil) {
     return SCRIPT_PREFIX + originalString + SCRIPT_SUFFIX;
    } else {
      return originalString;
    }
  }

  public Map<String, Map<String, String>> getAppData() {
    if (allData == null) {
      setupAppData();
    }
    return allData;
  }

  private void setupAppData() {
    allData = new HashMap<String, Map<String, String>>();

    Element root = fetchStateDocument().getDocumentElement();

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
      currentData.put(field, turnEvil(value));
    }
  }

  public void setAppData(String id, String key, String value) {
    if (allData == null) {
      setupAppData();
    }

    Map<String, String> personData = allData.get(id);
    if (personData == null) {
      personData = new HashMap<String, String>();
      allData.put(id, personData);
    }

    personData.put(key, value);
  }

  public Map<String, List<String>> getFriendIds() {
    if (friendIdMap == null) {
      setupPeopleData();
    }
    return friendIdMap;
  }

  public Map<String, Person> getAllPeople() {
    if (allPeople == null) {
      setupPeopleData();
    }
    return allPeople;
  }

  private void setupPeopleData() {
    Element root = fetchStateDocument().getDocumentElement();

    allPeople = new HashMap<String, Person>();
    friendIdMap = new HashMap<String, List<String>>();
    setupPeopleInXmlTag(root, "people");
  }

  // Adds all people in the xml tag to the allPeople map.
  // Also puts friends ids into the friendIdMap
  private void setupPeopleInXmlTag(Element root, String tagName) {
    // TODO: Use the opensource Collections library
    NodeList elements = root.getElementsByTagName(tagName);
    if (elements == null || elements.item(0) == null) {
      return;
    }

    NodeList personNodes = elements.item(0).getChildNodes();

    for (int i = 0; i < personNodes.getLength(); i++) {
      Node personNode = personNodes.item(i);
      NamedNodeMap attributes = personNode.getAttributes();
      if (attributes == null) {
        continue;
      }

      String name = attributes.getNamedItem("name").getNodeValue();
      String id = attributes.getNamedItem("id").getNodeValue();
      Person person = new Person(id, new Name(turnEvil(name)));

      Node phoneItem = attributes.getNamedItem("phone");
      if (phoneItem != null) {
        String phone = phoneItem.getNodeValue();
        List<Phone> phones = new ArrayList<Phone>();
        phones.add(new Phone(turnEvil(phone), null));
        person.setPhoneNumbers(phones);
      }

      Node genderItem = attributes.getNamedItem("gender");
      if (genderItem != null) {
        String gender = genderItem.getNodeValue();
        if ("F".equals(gender)) {
          person.setGender(new Enum<Enum.Gender>(Enum.Gender.FEMALE, "Female"));
        } else if ("M".equals(gender)) {
          person.setGender(new Enum<Enum.Gender>(Enum.Gender.MALE, "Male"));
        }
      } 

      allPeople.put(id, person);
      friendIdMap.put(id, getFriends(personNode));
    }
  }

  private List<String> getFriends(Node personNode) {
    List<String> friends = new ArrayList<String>();
    NodeList friendNodes = personNode.getChildNodes();
    for (int j = 0; j < friendNodes.getLength(); j++) {
      String friendId = friendNodes.item(j).getTextContent();
      if (friendId != null && friendId.trim().length() != 0) {
        friends.add(friendId.trim());
      }
    }
    return friends;
  }

  public Map<String, List<Activity>> getActivities() {
    if (allActivities == null) {
      setupActivities();
    }

    return allActivities;
  }

  private void setupActivities() {
    allActivities = new HashMap<String, List<Activity>>();

    Element root = fetchStateDocument().getDocumentElement();
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
        activity.setStreamTitle(turnEvil(streamTitle));
        activity.setTitle(turnEvil(title));
        activity.setBody(turnEvil(body));
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
