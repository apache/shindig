/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.shindig.social.opensocial.jpa.spi;

import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.EnumImpl;
import org.apache.shindig.social.opensocial.jpa.ActivityDb;
import org.apache.shindig.social.opensocial.jpa.AddressDb;
import org.apache.shindig.social.opensocial.jpa.ApplicationDataMapDb;
import org.apache.shindig.social.opensocial.jpa.ApplicationDb;
import org.apache.shindig.social.opensocial.jpa.BodyTypeDb;
import org.apache.shindig.social.opensocial.jpa.EmailDb;
import org.apache.shindig.social.opensocial.jpa.EnumDb;
import org.apache.shindig.social.opensocial.jpa.FriendDb;
import org.apache.shindig.social.opensocial.jpa.MediaItemDb;
import org.apache.shindig.social.opensocial.jpa.NameDb;
import org.apache.shindig.social.opensocial.jpa.OrganizationAddressDb;
import org.apache.shindig.social.opensocial.jpa.PersonAddressDb;
import org.apache.shindig.social.opensocial.jpa.PersonDb;
import org.apache.shindig.social.opensocial.jpa.PersonOrganizationDb;
import org.apache.shindig.social.opensocial.jpa.PhoneDb;
import org.apache.shindig.social.opensocial.jpa.PhotoDb;
import org.apache.shindig.social.opensocial.jpa.UrlDb;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.Drinker;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.LookingFor;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.NetworkPresence;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;

/**
 * 
 * Bootstrap class to setup a test database with some dummy data,
 * which is used by unit tests in spi package.
 *
 */
public class SpiDatabaseBootstrap {

  private final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
  
  private EntityManager entityManager;
  
  @Inject
  public SpiDatabaseBootstrap(EntityManager entityManager) {
    this.entityManager = entityManager;
  }
  
  /*
   * Init database bootstrap
   */
  public void init() throws Exception {
    this.bootstrapDatabase();    
  }
  
  /*
   * Bootstrap database with some dummy test data
   */
  protected void bootstrapDatabase() throws Exception {
    // Start transaction
    if (!entityManager.getTransaction().isActive()) {
      entityManager.getTransaction().begin();
    }
  
    // Build person with dummy data
    Person canonical = buildCanonicalPerson();    
    Person johnDoe = buildPerson("john.doe", "Johnny", Person.Gender.male, true, "Doe", "John", "John Doe");
    Person janeDoe = buildPerson("jane.doe", "Janey", Person.Gender.female, true, "Doe", "Jane", "Jane Doe"); 
    Person georgeDoe = buildPerson("george.doe", "Georgey", Person.Gender.male, true, "Doe", "George", "George Doe");
    Person mario = buildPerson("mario.rossi", "Mario", Person.Gender.male, true, "Rossi", "Mario", "Mario Rossi"); 
    Person maija = buildPerson("maija.m", "Maija", Person.Gender.female, true, "Meik\u00e4l\u00e4inen", "Maija", "Maija Meik\u00e4l\u00e4inen");
    
    // Persist each person
    entityManager.persist(canonical);
    entityManager.persist(johnDoe);
    entityManager.persist(janeDoe);    
    entityManager.persist(georgeDoe);
    entityManager.persist(mario);
    entityManager.persist(maija);
    
    // Build and persist friend relationships
    entityManager.persist(buildFriend(canonical, johnDoe));
    entityManager.persist(buildFriend(canonical, janeDoe));
    entityManager.persist(buildFriend(canonical, georgeDoe));
    entityManager.persist(buildFriend(canonical, maija));
    entityManager.persist(buildFriend(johnDoe, janeDoe));
    entityManager.persist(buildFriend(johnDoe, georgeDoe));
    entityManager.persist(buildFriend(johnDoe, maija));
    entityManager.persist(buildFriend(janeDoe, johnDoe));
    entityManager.persist(buildFriend(georgeDoe, johnDoe));
    
    // Build and persist activity test data    
    entityManager.persist(buildCanonicalActivity("canonical", "1"));
    entityManager.persist(buildCanonicalActivity("canonical", "2"));
    
    ActivityDb activity1 = buildActivityTemplate("john.doe", "1");    
    activity1.setTitle("yellow");
    activity1.setBody("what a color!");
    entityManager.persist(activity1);
    
    ActivityDb activity2 = buildActivityTemplate("jane.doe", "1");    
    activity2.setBody("and she thinks you look like him");
    List<MediaItem> mediaItems = new ArrayList<MediaItem>();
    MediaItemDb mediaItem1 = new MediaItemDb();
    mediaItem1.setMimeType("image/jpeg");
    mediaItem1.setType(MediaItem.Type.IMAGE);
    mediaItem1.setUrl("http://animals.nationalgeographic.com/staticfiles/NGS/Shared/StaticFiles/animals/images/primary/black-spider-monkey.jpg");
    MediaItemDb mediaItem2 = new MediaItemDb();
    mediaItem2.setMimeType("image/jpeg");
    mediaItem2.setType(MediaItem.Type.IMAGE);
    mediaItem2.setUrl("http://image.guardian.co.uk/sys-images/Guardian/Pix/gallery/2002/01/03/monkey300.jpg");
    mediaItems.add(mediaItem1);
    mediaItems.add(mediaItem2);
    activity2.setMediaItems(mediaItems);
    activity2.setStreamTitle("jane's photos");
    activity2.setTitle("Jane just posted a photo of a monkey");
    entityManager.persist(activity2);
    
    ActivityDb activity3 = buildActivityTemplate("jane.doe", "2");    
    activity3.setBody("or is it you?");
    List<MediaItem> mediaItems2 = new ArrayList<MediaItem>();
    MediaItemDb mediaItem3 = new MediaItemDb();
    mediaItem3.setMimeType("image/jpeg");
    mediaItem3.setType(MediaItem.Type.IMAGE);
    mediaItem3.setUrl("http://www.funnyphotos.net.au/images/fancy-dress-dog-yoda-from-star-wars1.jpg");
    mediaItems2.add(mediaItem3);
    activity3.setMediaItems(mediaItems2);
    activity3.setStreamTitle("jane's photos");
    activity3.setTitle("Jane says George likes yoda!");
    entityManager.persist(activity3);
    
    // Build and persist application data test data
    ApplicationDb testApplication = new ApplicationDb();
    testApplication.setId("app");
    entityManager.persist(testApplication);
    
    ApplicationDataMapDb applicationDataMap1 = buildApplicationDataTemplate(testApplication, "canonical", "2");
    applicationDataMap1.getValues().put("size", "100");
    entityManager.persist(applicationDataMap1);
    
    ApplicationDataMapDb applicationDataMap2 = buildApplicationDataTemplate(testApplication, "john.doe", "0");
    entityManager.persist(applicationDataMap2);
    
    ApplicationDataMapDb applicationDataMap3 = buildApplicationDataTemplate(testApplication, "george.doe", "2");
    entityManager.persist(applicationDataMap3);
    
    ApplicationDataMapDb applicationDataMap4 = buildApplicationDataTemplate(testApplication, "jane.doe", "7");
    entityManager.persist(applicationDataMap4);
    
    ApplicationDataMapDb applicationDataMap5 = buildApplicationDataTemplate(testApplication, "maija.m", null);
    entityManager.persist(applicationDataMap5);
   
    // Commit transaction
    entityManager.getTransaction().commit();
  }
  
  /**
   * Delete all previous data
   * 
   * @throws Exception
   */
  public void tearDown() throws Exception {
    // Start transaction
    if (!entityManager.getTransaction().isActive()) {
      entityManager.getTransaction().begin();
    }
    
    // Delete all data
    entityManager.createNativeQuery("delete from friend where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from activity_media where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from url where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from template_params where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from photo where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from phone where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from person_properties where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from person_organization where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from person_group where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from person_application where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from person_address where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from person_account where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from person where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from organizational_address where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from organization where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from name where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from message where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from membership where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from media_item where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from im where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from group_property where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from friend_property where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from email where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from body_type where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from application_property where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from application_datavalue where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from application_datamap where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from application where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from address where 1 > 0").executeUpdate();    
    entityManager.createNativeQuery("delete from activity where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from account where 1 > 0").executeUpdate();
    entityManager.createNativeQuery("delete from list_field where 1 > 0").executeUpdate();    
    
    // Commit transaction
    entityManager.getTransaction().commit();
    
    // Clear entity manager
    entityManager.clear();
  }
  
  //
  // Build methods that create dummy test data 
  //
  
  private ApplicationDataMapDb buildApplicationDataTemplate(ApplicationDb application, String personId, String count) {
    ApplicationDataMapDb applicationDataMap = new ApplicationDataMapDb();
    applicationDataMap.setApplication(application);
    applicationDataMap.setPersonId(personId);    
    Map<String, String> values = new MapMaker().makeMap();
    if (null != count) {
      values.put("count", count);
    }
    applicationDataMap.setValues(values);
    return applicationDataMap;
  }
  
  private ActivityDb buildCanonicalActivity(String userId, String id) {
    ActivityDb activity = buildActivityTemplate(userId, id);
    
    // Common attributes
    activity.setPriority(0.7F);
    activity.setStreamFaviconUrl("http://upload.wikimedia.org/wikipedia/commons/0/02/Nuvola_apps_edu_languages.gif");
    activity.setStreamSourceUrl("http://www.example.org/canonical/streamsource");
    activity.setStreamTitle("All my activities");
    activity.setStreamUrl("http://www.example.org/canonical/activities");
        
    // Set othe attributes depending on given id
    if ("1".equals(id)) {          
      activity.setBody("Went rafting");
      activity.setBodyId("1");
      activity.setExternalId("http://www.example.org/123456");
      List<MediaItem> mediaItems = new ArrayList<MediaItem>();
      MediaItemDb mediaItem1 = new MediaItemDb();
      mediaItem1.setMimeType("image/*");
      mediaItem1.setType(MediaItem.Type.IMAGE);
      mediaItem1.setUrl("http://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Rafting_em_Brotas.jpg/800px-Rafting_em_Brotas.jpg");
      MediaItemDb mediaItem2 = new MediaItemDb();
      mediaItem2.setMimeType("audio/mpeg");
      mediaItem2.setType(MediaItem.Type.AUDIO);
      mediaItem2.setUrl("http://www.archive.org/download/testmp3testfile/mpthreetest.mp3");
      mediaItems.add(mediaItem1);
      mediaItems.add(mediaItem2);
      activity.setMediaItems(mediaItems);
      activity.setPostedTime(1111111111L);
      Map<String, String> templateParams = new MapMaker().makeMap();
      templateParams.put("small", "true");
      templateParams.put("otherContent", "and got wet");
      activity.setTemplateParams(templateParams);
      activity.setTitle("My trip");
      activity.setTitleId("1");
      activity.setUpdated(new Date());
      activity.setUrl("http://www.example.org/canonical/activities/1");
      
    } else if ("2".equals(id)) {      
      activity.setBody("Went skiing");
      activity.setBodyId("2");
      activity.setExternalId("http://www.example.org/123457");
      List<MediaItem> mediaItems = new ArrayList<MediaItem>();
      activity.setMediaItems(mediaItems);
      activity.setPostedTime(1111111112L);
      Map<String, String> templateParams = new MapMaker().makeMap();
      templateParams.put("small", "true");
      templateParams.put("otherContent", "and went fast");
      activity.setTemplateParams(templateParams);
      activity.setTitle("My next trip");
      activity.setTitleId("2");
      activity.setUpdated(new Date());
      activity.setUrl("http://www.example.org/canonical/activities/2");
    }
    return activity;
  }

  private ActivityDb buildActivityTemplate(String userId, String id) {
    ActivityDb activity = new ActivityDb();
    activity.setUserId(userId);
    activity.setId(id);    
    return activity;
  }
  
  private FriendDb buildFriend(Person person, Person friend) {
    FriendDb friendDb = new FriendDb();
    friendDb.setPerson(person);
    friendDb.setFriend(friend);
    return friendDb;
  }

  private Person buildPerson(String id, String displayName, Person.Gender gender, boolean hasApp,
      String familyName, String givenName, String formatted) throws Exception {
    Person person = buildPersonTemplate(id);
    person.setDisplayName(displayName);
    person.setGender(gender);
    person.setHasApp(hasApp);
    
    NameDb name = new NameDb();
    name.setFamilyName(familyName);
    name.setGivenName(givenName);
    name.setFormatted(formatted);
    person.setName(name);
    
    return person;
  }
  
  private Person buildCanonicalPerson() throws Exception {
    Person person = buildPersonTemplate("canonical");
    person.setAboutMe("I have an example of every piece of data");    
    person.setActivities(asList("Coding Shindig"));    
    List<Address> addresses = new ArrayList<Address>();
    PersonAddressDb address = new PersonAddressDb();
    address.setCountry("US");
    address.setLatitude(28.3043F);
    address.setLongitude(143.0859F);
    address.setLocality("who knows");
    address.setPostalCode("12345");
    address.setRegion("Apache, CA");
    address.setStreetAddress("1 OpenStandards Way");
    address.setType("home");
    address.setFormatted("PoBox 3565, 1 OpenStandards Way, Apache, CA");
    // address.setPerson(person);
    addresses.add(address);
    person.setAddresses(addresses);    
    person.setAge(33);
    
    BodyTypeDb bodyType = new BodyTypeDb();
    bodyType.setBuild("svelte");
    bodyType.setEyeColor("blue");
    bodyType.setHairColor("black");
    bodyType.setHeight(1.84F);
    bodyType.setWeight(74F);
    person.setBodyType(bodyType);
    
    person.setBooks(asList("The Cathedral & the Bazaar","Catch 22"));
    person.setCars(asList("beetle","prius"));
    person.setChildren("3");
    
    AddressDb currentLocation = new AddressDb();
    currentLocation.setLatitude(48.858193F);
    currentLocation.setLongitude(2.29419F);
    person.setCurrentLocation(currentLocation);
        
    person.setBirthday(buildDate("1975-01-01"));
    person.setDisplayName("Shin Digg");
    person.setDrinker(new EnumDb<Drinker>(Drinker.SOCIALLY));
    
    List<ListField> emails = new ArrayList<ListField>();
    EmailDb email = new EmailDb();
    email.setValue("dev@shindig.apache.org");
    email.setType("work");
    emails.add(email);
    person.setEmails(emails);
   
    person.setEthnicity("developer");    
    person.setFashion("t-shirts");    
    person.setFood(asList("sushi","burgers"));    
    person.setGender(Person.Gender.male);
    person.setHappiestWhen("coding");    
    person.setHasApp(true);    
    person.setHeroes(asList("Doug Crockford", "Charles Babbage"));    
    person.setHumor("none to speak of");    
    person.setInterests(asList("PHP","Java"));    
    person.setJobInterests("will work for beer");
    
    List<Organization> organizations = new ArrayList<Organization>();
    
    PersonOrganizationDb organization1 = new PersonOrganizationDb();
    OrganizationAddressDb orgAddress1 = new OrganizationAddressDb();
    orgAddress1.setFormatted("1 Shindig Drive");
    organization1.setAddress(orgAddress1);
    organization1.setDescription("lots of coding");
    organization1.setEndDate(buildDate("2010-10-10"));
    organization1.setField("Software Engineering");
    organization1.setName("Apache.com");
    organization1.setSalary("$1000000000");
    organization1.setStartDate(buildDate("1995-01-01"));
    organization1.setSubField("Development");
    organization1.setTitle("Grand PooBah");
    organization1.setWebpage("http://shindig.apache.org/");
    organization1.setType("job");
    
    PersonOrganizationDb organization2 = new PersonOrganizationDb();
    OrganizationAddressDb orgAddress2 = new OrganizationAddressDb();
    orgAddress2.setFormatted("1 Skid Row");
    organization2.setAddress(orgAddress2);
    organization2.setDescription("");
    organization2.setEndDate(buildDate("1995-01-01"));
    organization2.setField("College");
    organization2.setName("School of hard Knocks");
    organization2.setSalary("$100");
    organization2.setStartDate(buildDate("1991-01-01"));
    organization2.setSubField("Lab Tech");
    organization2.setTitle("Gopher");
    organization2.setWebpage("");
    organization2.setType("job");
    
    organizations.add(organization1);
    organizations.add(organization2);
    person.setOrganizations(organizations);
    
    person.setLanguagesSpoken(asList("English","Dutch","Esperanto"));
    person.setUpdated(new Date());
    person.setLivingArrangement("in a house");
        
    List<Enum<LookingFor>> lookingFor = Lists.newArrayList();
    Enum<LookingFor> lookingForOne = new EnumImpl<LookingFor>(LookingFor.RANDOM);
    Enum<LookingFor> lookingForTwo = new EnumImpl<LookingFor>(LookingFor.NETWORKING);
    lookingFor.add(lookingForOne);
    lookingFor.add(lookingForTwo);
    person.setLookingFor(lookingFor);
    
    person.setMovies(asList("Iron Man", "Nosferatu"));
    person.setMusic(asList("Chieftains","Beck"));
    
    NameDb name = new NameDb();
    name.setAdditionalName("H");
    name.setFamilyName("Digg");
    name.setGivenName("Shin");
    name.setHonorificPrefix("Sir");
    name.setHonorificSuffix("Social Butterfly");
    name.setFormatted("Sir Shin H. Digg Social Butterfly");
    person.setName(name);
    
    person.setNetworkPresence(new EnumDb<NetworkPresence>(NetworkPresence.ONLINE));
    
    person.setNickname("diggy");
    person.setPets("dog,cat");
    
    List<ListField> phoneNumbers = new ArrayList<ListField>();
    PhoneDb phone1 = new PhoneDb();
    phone1.setValue("111-111-111");
    phone1.setType("work");
    PhoneDb phone2 = new PhoneDb();
    phone2.setValue("999-999-999");
    phone2.setType("mobile");
    phoneNumbers.add(phone1);
    phoneNumbers.add(phone2);
    person.setPhoneNumbers(phoneNumbers);
    
    person.setPoliticalViews("open leaning");    
    person.setProfileSong(buildUrl("http://www.example.org/songs/OnlyTheLonely.mp3", "Feelin' blue", "road"));
    person.setProfileUrl("http://www.example.org/?id=1");
    person.setProfileVideo(buildUrl("http://www.example.org/videos/Thriller.flv", "Thriller", "video"));
   
    person.setQuotes(asList("I am therfore I code", "Doh!"));
    person.setRelationshipStatus("married to my job");
    person.setReligion("druidic");
    person.setRomance("twice a year");
    person.setScaredOf("COBOL");
    person.setSexualOrientation("north");
    person.setSmoker(new EnumDb<Smoker>(Smoker.NO));    
    person.setSports(asList("frisbee","rugby"));
    person.setStatus("happy");
    person.setTags(asList("C#","JSON","template"));
    person.setUtcOffset(-8L);
    person.setTurnOffs(asList("lack of unit tests","cabbage"));
    person.setTurnOns(asList("well document code"));
    person.setTvShows(asList("House","Battlestar Galactica"));
    
    List<Url> urls = new ArrayList<Url>();
    urls.add(buildUrl("http://www.example.org/?id=1", "my profile", "Profile"));
    urls.add(buildUrl("http://www.example.org/pic/?id=1", "my awesome picture", "Thumbnail"));
    person.setUrls(urls);
    
    List<ListField> photos = new ArrayList<ListField>();
    PhotoDb photo = new PhotoDb();
    photo.setValue("http://www.example.org/pic/?id=1");
    photo.setType("thumbnail");
    photos.add(photo);
    person.setPhotos(photos);
    
    return person;
  }
  
  private Person buildPersonTemplate(String personId) {
    PersonDb person = new PersonDb();
    person.setId(personId);
    person.setAboutMe("");
    
    person.setActivities(asList(""));
    
    List<Address> addresses = new ArrayList<Address>();
    PersonAddressDb address = new PersonAddressDb();
    address.setCountry("");
    address.setLatitude(0F);
    address.setLongitude(0F);
    address.setLocality("");
    address.setPostalCode("");
    address.setRegion("");
    address.setStreetAddress("");
    address.setType("");
    address.setFormatted("");
    // TODO This causes problems when converting back to json.
    // address.setPerson(person);
    addresses.add(address);
    person.setAddresses(addresses);
    
    person.setAge(0);
    
    BodyTypeDb bodyType = new BodyTypeDb();
    bodyType.setBuild("");
    bodyType.setEyeColor("");
    bodyType.setHairColor("");
    bodyType.setHeight(0F);
    bodyType.setWeight(0F);
    person.setBodyType(bodyType);
    
    person.setBooks(asList(""));
    person.setCars(asList(""));
    person.setChildren("");
    
    AddressDb currentLocation = new AddressDb();
    currentLocation.setLatitude(0F);
    currentLocation.setLongitude(0F);
    person.setCurrentLocation(currentLocation);
    
    person.setBirthday(new Date());
    person.setDisplayName("");
    person.setDrinker(new EnumDb<Drinker>(Drinker.SOCIALLY));
    
    List<ListField> emails = new ArrayList<ListField>();
    EmailDb email = new EmailDb();
    email.setValue("");
    email.setType("");
    emails.add(email);
    person.setEmails(emails);
    
    person.setEthnicity("");    
    person.setFashion("");    
    person.setFood(asList(""));    
    person.setGender(Person.Gender.male);
    person.setHappiestWhen("");    
    person.setHasApp(true);    
    person.setHeroes(asList(""));    
    person.setHumor("");    
    person.setInterests(asList(""));    
    person.setJobInterests("");
    
    List<Organization> organizations = new ArrayList<Organization>();
    
    PersonOrganizationDb organization1 = new PersonOrganizationDb();
    OrganizationAddressDb orgAddress1 = new OrganizationAddressDb();
    orgAddress1.setFormatted("");
    organization1.setAddress(orgAddress1);
    organization1.setDescription("");
    organization1.setEndDate(new Date());
    organization1.setField("");
    organization1.setName("");
    organization1.setSalary("");
    organization1.setStartDate(new Date());
    organization1.setSubField("");
    organization1.setTitle("");
    organization1.setWebpage("");
    organization1.setType("");    
    
    organizations.add(organization1);
    person.setOrganizations(organizations);
    
    person.setLanguagesSpoken(asList(""));
    person.setUpdated(new Date());
    person.setLivingArrangement("");
        
    List<Enum<LookingFor>> lookingFor = Lists.newArrayList();
    Enum<LookingFor> lookingForOne = new EnumImpl<LookingFor>(LookingFor.RANDOM);
    Enum<LookingFor> lookingForTwo = new EnumImpl<LookingFor>(LookingFor.NETWORKING);
    lookingFor.add(lookingForOne);
    lookingFor.add(lookingForTwo);
    person.setLookingFor(lookingFor);
    
    person.setMovies(asList(""));
    person.setMusic(asList(""));
    
    NameDb name = new NameDb();
    name.setAdditionalName("");
    name.setFamilyName("");
    name.setGivenName("");
    name.setHonorificPrefix("");
    name.setHonorificSuffix("");
    name.setFormatted("");
    person.setName(name);
    
    person.setNetworkPresence(new EnumDb<NetworkPresence>(NetworkPresence.ONLINE));
    person.setNickname("");
    person.setPets("");
    
    List<ListField> phoneNumbers = new ArrayList<ListField>();
    PhoneDb phone1 = new PhoneDb();
    phone1.setValue("");
    phone1.setType("");
    PhoneDb phone2 = new PhoneDb();
    phone2.setValue("");
    phone2.setType("");
    phoneNumbers.add(phone1);
    phoneNumbers.add(phone2);
    person.setPhoneNumbers(phoneNumbers);
    
    person.setPoliticalViews("");    
    person.setProfileSong(buildUrl("", "Link Text", "URL"));
    person.setProfileUrl("");
    person.setProfileVideo(buildUrl("", "Link Text", "URL"));
    person.setQuotes(asList(""));
    person.setRelationshipStatus("");
    person.setReligion("");
    person.setRomance("");
    person.setScaredOf("");
    person.setSexualOrientation("");
    person.setSmoker(new EnumDb<Smoker>(Smoker.NO));    
    person.setSports(asList(""));
    person.setStatus("");
    person.setTags(asList(""));
    person.setUtcOffset(-8L);
    person.setTurnOffs(asList(""));
    person.setTurnOns(asList(""));
    person.setTvShows(asList(""));
    
    List<Url> urls = new ArrayList<Url>();
    urls.add(buildUrl("", "", "Profile"));
    urls.add(buildUrl("", "", "Thumbnail"));
    person.setUrls(urls);
    
    List<ListField> photos = new ArrayList<ListField>();
    PhotoDb photo = new PhotoDb();
    photo.setValue("");
    photo.setType("thumbnail");
    photos.add(photo);
    person.setPhotos(photos);
    
    return person;
  }
  
  private Date buildDate(String dateAsString) throws Exception {    
    return DATE_FORMATTER.parse(dateAsString);
  }
  
  private List<String> asList(String... items) {
    return Arrays.asList(items);
  }
  
  private Url buildUrl(String targetUrl, String linkTest, String type) {
    Url url = new UrlDb();
    url.setValue(targetUrl);
    url.setLinkText(linkTest);
    url.setType(type);
    return url;
  }
  
}
