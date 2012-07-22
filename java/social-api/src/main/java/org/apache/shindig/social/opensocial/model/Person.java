/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.PersonImpl;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.ImplementedBy;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * see <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Person.Field">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Person.Field</a>
 * for all field meanings. All fields are represented in the js api at this time except for lastUpdated.
 * This field is currently only in the RESTful spec.
 *
 */
@ImplementedBy(PersonImpl.class)
@Exportablebean
public interface Person {
  /**
   * The type of a profile url when represented as a list field.
   */
  String PROFILE_URL_TYPE = "profile";

  /**
   * The type of thumbnail photo types when represented as list fields.
   */
  String THUMBNAIL_PHOTO_TYPE = "thumbnail";

  /**
   * The display name for the user.
   * @return the display name
   */
  String getDisplayName();

  /**
   * Set the display name.
   * @param displayName the new display name.
   */
  void setDisplayName(String displayName);

  /**
   * Enumeration of genders.
   */
  public enum Gender {
    /**
     * Female.
     */
    female,
    /**
     * Male.
     */
    male
  }

  /**
   * The fields that represent the person object in json form.
   */
  public static enum Field {
    /** the json field for aboutMe. */
    ABOUT_ME("aboutMe"),
    /** the json field for accounts. */
    ACCOUNTS("accounts"),
    /** the json field for activities. */
    ACTIVITIES("activities"),
    /** the json field for addresses. */
    ADDRESSES("addresses"),
    /** the json field for age. */
    AGE("age"),
    /** the json field for appData. */
    APP_DATA("appData"),
    /** the json field for bodyType. */
    BODY_TYPE("bodyType"),
    /** the json field for books. */
    BOOKS("books"),
    /** the json field for cars. */
    CARS("cars"),
    /** the json field for children. */
    CHILDREN("children"),
    /** the json field for currentLocation. */
    CURRENT_LOCATION("currentLocation"),
    /** the json field for birthday. */
    BIRTHDAY("birthday"),
    /** the json field for display name. */
    DISPLAY_NAME("displayName"), /** Needed to support the RESTful api. */
    /** the json field for drinker. */
    DRINKER("drinker"),
    /** the json field for emails. */
    EMAILS("emails"),
    /** the json field for ethnicity. */
    ETHNICITY("ethnicity"),
    /** the json field for fashion. */
    FASHION("fashion"),
    /** the json field for food. */
    FOOD("food"),
    /** the json field for gender. */
    GENDER("gender"),
    /** the json field for happiestWhen. */
    HAPPIEST_WHEN("happiestWhen"),
    /** the json field for hasApp. */
    HAS_APP("hasApp"),
    /** the json field for heroes. */
    HEROES("heroes"),
    /** the json field for humor. */
    HUMOR("humor"),
    /** the json field for id. */
    ID("id"),
    /** the json field for IM accounts. */
    IMS("ims"),
    /** the json field for interests. */
    INTERESTS("interests"),
    /** the json field for jobInterests. */
    JOB_INTERESTS("jobInterests"),
    /** the json field for languagesSpoken. */
    LANGUAGES_SPOKEN("languagesSpoken"),
    /** the json field for updated. */
    LAST_UPDATED("updated"), /** Needed to support the RESTful api. */
    /** the json field for livingArrangement. */
    LIVING_ARRANGEMENT("livingArrangement"),
    /** the json field for lookingFor. */
    LOOKING_FOR("lookingFor"),
    /** the json field for movies. */
    MOVIES("movies"),
    /** the json field for music. */
    MUSIC("music"),
    /** the json field for name. */
    NAME("name"),
    /** the json field for networkPresence. */
    NETWORKPRESENCE("networkPresence"),
    /** the json field for nickname. */
    NICKNAME("nickname"),
    /** the json field for organiztions. */
    ORGANIZATIONS("organizations"),
    /** the json field for pets. */
    PETS("pets"),
    /** the json field for phoneNumbers. */
    PHONE_NUMBERS("phoneNumbers"),
    /** the json field for photos. */
    PHOTOS("photos"),
    /** the json field for politicalViews. */
    POLITICAL_VIEWS("politicalViews"),
    /** the json field for preferredUsername */
    PREFERRED_USERNAME("preferredUsername"),
    /** the json field for profileSong. */
    PROFILE_SONG("profileSong"),
    /** the json field for profileUrl. */
    PROFILE_URL("profileUrl"),
    /** the json field for profileVideo. */
    PROFILE_VIDEO("profileVideo"),
    /** the json field for quotes. */
    QUOTES("quotes"),
    /** the json field for relationshipStatus. */
    RELATIONSHIP_STATUS("relationshipStatus"),
    /** the json field for religion. */
    RELIGION("religion"),
    /** the json field for romance. */
    ROMANCE("romance"),
    /** the json field for scaredOf. */
    SCARED_OF("scaredOf"),
    /** the json field for sexualOrientation. */
    SEXUAL_ORIENTATION("sexualOrientation"),
    /** the json field for smoker. */
    SMOKER("smoker"),
    /** the json field for sports. */
    SPORTS("sports"),
    /** the json field for status. */
    STATUS("status"),
    /** the json field for tags. */
    TAGS("tags"),
    /** the json field for thumbnailUrl. */
    THUMBNAIL_URL("thumbnailUrl"),
    /** the json field for utcOffset. */
    UTC_OFFSET("utcOffset"),
    /** the json field for turnOffs. */
    TURN_OFFS("turnOffs"),
    /** the json field for turnOns. */
    TURN_ONS("turnOns"),
    /** the json field for tvShows. */
    TV_SHOWS("tvShows"),
    /** the json field for urls. */
    URLS("urls");

    /**
     * a Map to convert json string to Field representations.
     */

    private static final Map<String,Field> LOOKUP = Maps.uniqueIndex(EnumSet.allOf(Field.class),
        Functions.toStringFunction());

    /**
     * The json field that the instance represents.
     */
    private final String urlString;

    /**
     * The set of all fields.
     */
    public static final Set<String> ALL_FIELDS = LOOKUP.keySet();

    /**
     * The set of default fields returned fields.
     */
    public static final Set<String> DEFAULT_FIELDS = ImmutableSet.of(
        ID.toString(),
        NAME.toString(),
        THUMBNAIL_URL.toString());

    /**
     * create a field base on the a json element.
     *
     * @param urlString the name of the element
     */
    private Field(String urlString) {
      this.urlString = urlString;
    }

    /**
     * emit the field as a json element.
     *
     * @return the field name
     */
    @Override
    public String toString() {
      return this.urlString;
    }

    public static Field getField(String jsonString) {
      return LOOKUP.get(jsonString);
    }

    /**
     * Converts from a url string (usually passed in the fields= parameter) into the
     * corresponding field enum.
     * @param urlString The string to translate.
     * @return The corresponding person field.
     */
    public static Person.Field fromUrlString(String urlString) {
      return LOOKUP.get(urlString);
    }
  }

  /**
   * Get a general statement about the person, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @return the value of aboutMe
   */
  String getAboutMe();

  /**
   * Set a general statement about the person, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @param aboutMe the value of aboutMe
   */
  void setAboutMe(String aboutMe);

  /**
   * Get the list of online accounts held by this person.
   * @return a list of Account objects
   */
  List<Account> getAccounts();

  /**
   * Set the list of online accounts held by this person.
   * @param accounts a list of Account objects
   */
  void setAccounts(List<Account> accounts);

  /**
   * Get the person's favorite activities, specified as an List of strings. Container support for
   * this field is OPTIONAL.
   *
   * @return list of activities.
   */
  List<String> getActivities();

  /**
   * Set the person's favorite activities, specified as an List of strings.
   *
   * @param activities a list of activities
   */
  void setActivities(List<String> activities);

  /**
   * Get addresses associated with the person, specified as an List of Address objects. Container
   * support for this field is OPTIONAL.
   *
   * @return a List of address objects
   */
  List<Address> getAddresses();

  /**
   * Set addresses associated with the person, specified as an List of Address objects. Container
   * support for this field is OPTIONAL.
   *
   * @param addresses a list of address objects
   */
  void setAddresses(List<Address> addresses);

  /**
   * Get the person's age, specified as a number. Container support for this field is OPTIONAL.
   *
   * @return the persons age
   */
  Integer getAge();

  /**
   * Set the person's age, specified as a number. Container support for this field is OPTIONAL.
   *
   * @param age the persons age
   */
  void setAge(Integer age);

  /**
   * Get app data for the person.
   *
   * @return the app data, possibly a subset.
   */
  Map<String, ?> getAppData();

  /**
   * Sets app data for the person.
   *
   * @param appData the app data, possibly a subset
   */
  void setAppData(Map<String, ?> appData);

  /**
   * Get the person's date of birth, specified as a {@link Date} object. Container support for this
   * field is OPTIONAL.
   *
   * @return the person's data of birth
   */
  Date getBirthday();

  /**
   * Set the person's date of birth, specified as a {@link Date} object. Container support for this
   * field is OPTIONAL.
   *
   * @param birthday the person's data of birth
   */
  void setBirthday(Date birthday);

  /**
   * Get the person's body characteristics, specified as an BodyType. Container support for this
   * field is OPTIONAL.
   *
   * @return the BodyType
   */
  BodyType getBodyType();

  /**
   * Set the person's body characteristics, specified as an BodyType. Container support for this
   * field is OPTIONAL.
   *
   * @param bodyType the person's BodyType
   */
  void setBodyType(BodyType bodyType);

  /**
   * Get the person's favorite books, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @return list of books as strings
   */
  List<String> getBooks();

  /**
   * Set the person's favorite books, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @param books a list of the person's books
   */
  void setBooks(List<String> books);

  /**
   * Get the person's favorite cars, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @return the persons favorite cars
   */
  List<String> getCars();

  /**
   * Set the person's favorite cars, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @param cars a list of the persons favorite cars
   */
  void setCars(List<String> cars);

  /**
   * Get a description of the person's children, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @return the persons children
   */
  String getChildren();

  /**
   * Set a description of the person's children, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @param children the persons children
   */
  void setChildren(String children);

  /**
   * Get the person's current location, specified as an {@link Address}. Container support for this
   * field is OPTIONAL.
   *
   * @return the persons current location
   */
  Address getCurrentLocation();

  /**
   * Set the person's current location, specified as an {@link Address}. Container support for this
   * field is OPTIONAL.
   *
   * @param currentLocation the persons current location
   */
  void setCurrentLocation(Address currentLocation);

  /**
   * Get the person's drinking status, specified as an {@link Enum} with the enum's key referencing
   * {@link Drinker}. Container support for this field is OPTIONAL.
   *
   * @return the persons drinking status
   */
  Enum<Drinker> getDrinker();

  /**
   * Get the person's drinking status, specified as an {@link Enum} with the enum's key referencing
   * {@link Drinker}. Container support for this field is OPTIONAL.
   *
   * @param newDrinker the persons drinking status
   */
  void setDrinker(Enum<Drinker> newDrinker);

  /**
   * Get the person's Emails associated with the person.
   * Container support for this field is OPTIONAL.
   *
   * @return a list of the person's emails
   */
  List<ListField> getEmails();

  /**
   * Set the person's Emails associated with the person.
   * Container support for this field is OPTIONAL.
   *
   * @param emails a list of the person's emails
   */
  void setEmails(List<ListField> emails);

  /**
   * Get the person's ethnicity, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @return the person's ethnicity
   */
  String getEthnicity();

  /**
   * Set the person's ethnicity, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @param ethnicity the person's ethnicity
   */
  void setEthnicity(String ethnicity);

  /**
   * Get the person's thoughts on fashion, specified as a string. Container support for this field
   * is OPTIONAL.
   *
   * @return the person's thoughts on fashion
   */
  String getFashion();

  /**
   * Set the person's thoughts on fashion, specified as a string. Container support for this field
   * is OPTIONAL.
   *
   * @param fashion the person's thoughts on fashion
   */
  void setFashion(String fashion);

  /**
   * Get the person's favorite food, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @return the person's favorite food
   */
  List<String> getFood();

  /**
   * Set the person's favorite food, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @param food the person's favorite food
   */
  void setFood(List<String> food);

  /**
   * Get a person's gender, specified as an {@link Gender}.
   *
   * @return the person's gender
   */
  Gender getGender();

  /**
   * Set a person's gender, specified as an {@link Gender}.
   *
   * @param newGender the person's gender
   */
  void setGender(Gender newGender);

  /**
   * Get a description of when the person is happiest, specified as a string. Container support for
   * this field is OPTIONAL.
   *
   * @return a description of when the person is happiest
   */
  String getHappiestWhen();

  /**
   * Set a description of when the person is happiest, specified as a string. Container support for
   * this field is OPTIONAL.
   *
   * @param happiestWhen a description of when the person is happiest
   */
  void setHappiestWhen(String happiestWhen);

  /**
   * Get if the person has used the current app. Container support for this field is OPTIONAL.
   * Has app needs to take account of the context of the application that is performing the
   * query on this person object.
   * @return true the current app has been used
   */
  Boolean getHasApp();

  /**
   * Set if the person has used the current app. Container support for this field is OPTIONAL.
   *
   * @param hasApp set true the current app has been used
   */
  void setHasApp(Boolean hasApp);

  /**
   * Get a person's favorite heroes, specified as an Array of strings. Container support for this
   * field is OPTIONAL.
   *
   * @return a list of the person's favorite heroes
   */
  List<String> getHeroes();

  /**
   * Set a person's favorite heroes, specified as an Array of strings. Container support for this
   * field is OPTIONAL.
   *
   * @param heroes a list of the person's favorite heroes
   */
  void setHeroes(List<String> heroes);

  /**
   * Get the person's thoughts on humor, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @return the person's thoughts on humor
   */
  String getHumor();

  /**
   * Set the person's thoughts on humor, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @param humor the person's thoughts on humor
   */
  void setHumor(String humor);

  /**
   * Get A string ID that can be permanently associated with this person. Container support for this
   * field is REQUIRED.
   *
   * @return the permanent ID of the person
   */
  String getId();

  /**
   * Set A string ID that can be permanently associated with this person. Container support for this
   * field is REQUIRED.
   *
   * @param id the permanent ID of the person
   */
  void setId(String id);

  /**
   * Get a list of Instant messaging address for this Person. No official canonicalization rules
   * exist for all instant messaging addresses, but Service Providers SHOULD remove all whitespace
   * and convert the address to lowercase, if this is appropriate for the service this IM address is
   * used for. Instead of the standard Canonical Values for type, this field defines the following
   * Canonical Values to represent currently popular IM services: aim, gtalk, icq, xmpp, msn, skype,
   * qq, and yahoo.
   *
   * @return A list of IM addresses
   */
  List<ListField> getIms();

  /**
   * Set a list of Instant messaging address for this Person. No official canonicalization rules
   * exist for all instant messaging addresses, but Service Providers SHOULD remove all whitespace
   * and convert the address to lowercase, if this is appropriate for the service this IM address is
   * used for. Instead of the standard Canonical Values for type, this field defines the following
   * Canonical Values to represent currently popular IM services: aim, gtalk, icq, xmpp, msn, skype,
   * qq, and yahoo.
   *
   * @param ims a list ListFields representing IM addresses.
   */
  void setIms(List<ListField> ims);

  /**
   * Get the person's interests, hobbies or passions, specified as an List of strings. Container
   * support for this field is OPTIONAL.
   *
   * @return the person's interests, hobbies or passions
   */
  List<String> getInterests();

  /**
   * Set the person's interests, hobbies or passions, specified as an List of strings. Container
   * support for this field is OPTIONAL.
   *
   * @param interests the person's interests, hobbies or passions
   */
  void setInterests(List<String> interests);

  /**
   * Get the Person's favorite jobs, or job interests and skills, specified as a string. Container
   * support for this field is OPTIONAL
   *
   * @return the Person's favorite jobs, or job interests and skills
   */
  String getJobInterests();

  /**
   * Set the Person's favorite jobs, or job interests and skills, specified as a string. Container
   * support for this field is OPTIONAL
   *
   * @param jobInterests the Person's favorite jobs, or job interests and skills
   */
  void setJobInterests(String jobInterests);

  /**
   * Get a List of the languages that the person speaks as ISO 639-1 codes, specified as an List of
   * strings. Container support for this field is OPTIONAL.
   *
   * @return a List of the languages that the person speaks
   */
  List<String> getLanguagesSpoken();

  /**
   * Set a List of the languages that the person speaks as ISO 639-1 codes, specified as an List of
   * strings. Container support for this field is OPTIONAL.
   *
   * @param languagesSpoken a List of the languages that the person speaks
   */
  void setLanguagesSpoken(List<String> languagesSpoken);

  /**
   * The time this person was last updated.
   *
   * @return the last update time
   */
  Date getUpdated();

  /**
   * Set the time this record was last updated.
   *
   * @param updated the last update time
   */
  void setUpdated(Date updated);

  /**
   * Get a description of the person's living arrangement, specified as a string. Container support
   * for this field is OPTIONAL.
   *
   * @return a description of the person's living arrangement
   */
  String getLivingArrangement();

  /**
   * Set a description of the person's living arrangement, specified as a string. Container support
   * for this field is OPTIONAL.
   *
   * @param livingArrangement a description of the person's living arrangement
   */
  void setLivingArrangement(String livingArrangement);

  /**
   * Get a person's statement about who or what they are looking for, or what they are interested in
   * meeting people for. Specified as an List of {@link org.apache.shindig.protocol.model.Enum} with the enum's key referencing
   * {@link LookingFor} Container support for this field is OPTIONAL.
   *
   * @return person's statement about who or what they are looking for
   */
  List<Enum<LookingFor>> getLookingFor();

  /**
   * Get a person's statement about who or what they are looking for, or what they are interested in
   * meeting people for. Specified as an List of {@link Enum} with the enum's key referencing
   * {@link LookingFor} Container support for this field is OPTIONAL.
   *
   * @param lookingFor person's statement about who or what they are looking for
   */
  void setLookingFor(List<Enum<LookingFor>> lookingFor);

  /**
   * Get the Person's favorite movies, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @return the Person's favorite movies
   */
  List<String> getMovies();

  /**
   * Set the Person's favorite movies, specified as an List of strings. Container support for this
   * field is OPTIONAL.
   *
   * @param movies the Person's favorite movies
   */
  void setMovies(List<String> movies);

  /**
   * Get the Person's favorite music, specified as an List of strings Container support for this
   * field is OPTIONAL.
   *
   * @return Person's favorite music
   */
  List<String> getMusic();

  /**
   * Set the Person's favorite music, specified as an List of strings Container support for this
   * field is OPTIONAL.
   *
   * @param music Person's favorite music
   */
  void setMusic(List<String> music);

  /**
   * Get the person's name Container support for this field is REQUIRED.
   *
   * @return the person's name
   */
  Name getName();

  /**
   * Set the person's name Container support for this field is REQUIRED.
   *
   * @param name the person's name
   */
  void setName(Name name);

  /**
   * Get the person's current network status. Specified as an {@link Enum} with the enum's key
   * referencing {@link NetworkPresence}. Container support for this field is OPTIONAL.
   *
   * @return the person's current network status
   */
  Enum<NetworkPresence> getNetworkPresence();

  /**
   * Set the person's current network status. Specified as an {@link org.apache.shindig.protocol.model.Enum} with the enum's key
   * referencing {@link NetworkPresence}. Container support for this field is OPTIONAL.
   *
   * @param networkPresence the person's current network status
   */
  void setNetworkPresence(org.apache.shindig.protocol.model.Enum<NetworkPresence> networkPresence);

  /**
   * Get the person's nickname. Container support for this field is REQUIRED.
   *
   * @return the person's nickname.
   */
  String getNickname();

  /**
   * Set the the person's nickname. Container support for this field is REQUIRED.
   *
   * @param nickname the person's nickname.
   */
  void setNickname(String nickname);

  /**
   * Get a list of current or past organizational affiliations of this Person.
   * @return a list of Organization objects
   */
  List<Organization> getOrganizations();

  /**
   * Set a list of current or past organizational affiliations of this Person.
   * @param organizations a list of Organisation objects
   */
  void setOrganizations(List<Organization> organizations);

  /**
   * Get a description of the person's pets Container support for this field is OPTIONAL.
   *
   * @return a description of the person's pets
   */
  String getPets();

  /**
   * Set a description of the person's pets Container support for this field is OPTIONAL.
   *
   * @param pets a description of the person's pets
   */
  void setPets(String pets);

  /**
   * Get the Phone numbers associated with the person.
   *
   * @return the Phone numbers associated with the person
   */
  List<ListField> getPhoneNumbers();

  /**
   * Set the Phone numbers associated with the person.
   *
   * @param phoneNumbers the Phone numbers associated with the person
   */
  void setPhoneNumbers(List<ListField> phoneNumbers);

  /**
   * URL of a photo of this person. The value SHOULD be a canonicalized URL, and MUST point to an
   * actual image file (e.g. a GIF, JPEG, or PNG image file) rather than to a web page containing an
   * image. Service Providers MAY return the same image at different sizes, though it is recognized
   * that no standard for describing images of various sizes currently exists. Note that this field
   * SHOULD NOT be used to send down arbitrary photos taken by this user, but specifically profile
   * photos of the contact suitable for display when describing the contact.
   *
   * @return a list of Photos
   */
  List<ListField> getPhotos();

  /**
   * Set a list of Photos for the person.
   * @see Person#getPhotos()
   *
   * @param photos a list of photos.
   */
  void setPhotos(List<ListField> photos);

  /**
   * Get the Person's political views, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @return the Person's political views
   */
  String getPoliticalViews();

  /**
   * Set the Person's political views, specified as a string. Container support for this field is
   * OPTIONAL.
   *
   * @param politicalViews the Person's political views
   */
  void setPoliticalViews(String politicalViews);

  /**
   * Get the Person's preferred username, specified as a string. Container support for this field is OPTIONAL
   *
   * @return the Person's preferred username
   */
  String getPreferredUsername();

  /**
   * Set the Person's preferred username, specified as a string. Container support for this field is OPTIONAL
   *
   * @param preferredString the Person's preferred username
   */
  void setPreferredUsername(String preferredString);

  /**
   * Get the Person's profile song, specified as an {@link Url}. Container support for this field
   * is OPTIONAL.
   *
   * @return the Person's profile song
   */
  Url getProfileSong();

  /**
   * Set the Person's profile song, specified as an {@link Url}. Container support for this field
   * is OPTIONAL.
   *
   * @param profileSong the Person's profile song
   */
  void setProfileSong(Url profileSong);

  /**
   * Get the Person's profile video. Container support for this field is OPTIONAL.
   *
   * @return the Person's profile video
   */
  Url getProfileVideo();

  /**
   * Set the Person's profile video. Container support for this field is OPTIONAL.
   *
   * @param profileVideo the Person's profile video
   */
  void setProfileVideo(Url profileVideo);

  /**
   * Get the person's favorite quotes Container support for this field is OPTIONAL.
   *
   * @return the person's favorite quotes
   */
  List<String> getQuotes();

  /**
   * Set the person's favorite quotes. Container support for this field is OPTIONAL.
   *
   * @param quotes the person's favorite quotes
   */
  void setQuotes(List<String> quotes);

  /**
   * Get the person's relationship status. Container support for this field is OPTIONAL.
   *
   * @return the person's relationship status
   */
  String getRelationshipStatus();

  /**
   * Set the person's relationship status. Container support for this field is OPTIONAL.
   *
   * @param relationshipStatus the person's relationship status
   */
  void setRelationshipStatus(String relationshipStatus);

  /**
   * Get the person's relgion or religious views. Container support for this field is OPTIONAL.
   *
   * @return the person's relgion or religious views
   */
  String getReligion();

  /**
   * Set the person's relgion or religious views. Container support for this field is OPTIONAL.
   *
   * @param religion the person's relgion or religious views
   */
  void setReligion(String religion);

  /**
   * Get the person's comments about romance. Container support for this field is OPTIONAL.
   *
   * @return the person's comments about romance,
   */
  String getRomance();

  /**
   * Set a the person's comments about romance, Container support for this field is OPTIONAL.
   *
   * @param romance the person's comments about romance,
   */
  void setRomance(String romance);

  /**
   * Get what the person is scared of Container support for this field is OPTIONAL.
   *
   * @return what the person is scared of
   */
  String getScaredOf();

  /**
   * Set what the person is scared of Container support for this field is OPTIONAL.
   *
   * @param scaredOf what the person is scared of
   */
  void setScaredOf(String scaredOf);

  /**
   * Get the person's sexual orientation. Container support for this field is OPTIONAL.
   *
   * @return the person's sexual orientation
   */
  String getSexualOrientation();

  /**
   * Set the person's sexual orientation Container support for this field is OPTIONAL.
   *
   * @param sexualOrientation the person's sexual orientation
   */
  void setSexualOrientation(String sexualOrientation);

  /**
   * Get the person's smoking status. Container support for this field is OPTIONAL.
   *
   * @return the person's smoking status
   */
  Enum<Smoker> getSmoker();

  /**
   * Set the person's smoking status. Container support for this field is OPTIONAL.
   *
   * @param newSmoker the person's smoking status
   */
  void setSmoker(Enum<Smoker> newSmoker);

  /**
   * Get the person's favorite sports. Container support for this field is OPTIONAL.
   *
   * @return the person's favorite sports
   */
  List<String> getSports();

  /**
   * Set the person's favorite sports. Container support for this field is OPTIONAL.
   *
   * @param sports the person's favorite sports
   */
  void setSports(List<String> sports);

  /**
   * Get the person's status, headline or shoutout. Container support for this field is OPTIONAL.
   *
   * @return the person's status, headline or shoutout
   */
  String getStatus();

  /**
   * Set the person's status, headline or shoutout. Container support for this field is OPTIONAL.
   *
   * @param status the person's status, headline or shoutout
   */
  void setStatus(String status);

  /**
   * Get arbitrary tags about the person. Container support for this field is OPTIONAL.
   *
   * @return arbitrary tags about the person.
   */
  List<String> getTags();

  /**
   * Set arbitrary tags about the person. Container support for this field is OPTIONAL.
   *
   * @param tags arbitrary tags about the person.
   */
  void setTags(List<String> tags);

  /**
   * Get the Person's time zone, specified as the difference in minutes between Greenwich Mean Time
   * (GMT) and the user's local time. Container support for this field is OPTIONAL.
   *
   * @return the Person's time zone
   */
  Long getUtcOffset();

  /**
   * Set the Person's time zone, specified as the difference in minutes between Greenwich Mean Time
   * (GMT) and the user's local time. Container support for this field is OPTIONAL.
   *
   * @param utcOffset the Person's time zone
   */
  void setUtcOffset(Long utcOffset);

  /**
   * Get the person's turn offs. Container support for this field is OPTIONAL.
   *
   * @return the person's turn offs
   */
  List<String> getTurnOffs();

  /**
   * Set the person's turn offs. Container support for this field is OPTIONAL.
   *
   * @param turnOffs the person's turn offs
   */
  void setTurnOffs(List<String> turnOffs);

  /**
   * Get the person's turn ons. Container support for this field is OPTIONAL.
   *
   * @return the person's turn ons
   */
  List<String> getTurnOns();

  /**
   * Set the person's turn ons. Container support for this field is OPTIONAL.
   *
   * @param turnOns the person's turn ons
   */
  void setTurnOns(List<String> turnOns);

  /**
   * Get the person's favorite TV shows. Container support for this field is OPTIONAL.
   *
   * @return the person's favorite TV shows.
   */
  List<String> getTvShows();

  /**
   * Set the person's favorite TV shows. Container support for this field is OPTIONAL.
   *
   * @param tvShows the person's favorite TV shows.
   */
  void setTvShows(List<String> tvShows);

  /**
   * Get the URLs related to the person, their webpages, or feeds Container support for this field
   * is OPTIONAL.
   *
   * @return the URLs related to the person, their webpages, or feeds
   */
  List<Url> getUrls();

  /**
   * Set the URLs related to the person, their webpages, or feeds Container support for this field
   * is OPTIONAL.
   *
   * @param urls the URLs related to the person, their webpages, or feeds
   */
  void setUrls(List<Url> urls);

  /**
   * @return true if this person object represents the owner of the current page.
   */
  boolean getIsOwner();

  /**
   * Set the owner flag.
   * @param isOwner the isOwnerflag
   */
  void setIsOwner(boolean isOwner);

  /**
   * Returns true if this person object represents the currently logged in user.
   * @return true if the person accessing this object is a viewer.
   */
  boolean getIsViewer();

  /**
   * Returns true if this person object represents the currently logged in user.
   * @param isViewer the isViewer Flag
   */
  void setIsViewer(boolean isViewer);


  // Proxied fields

  /**
   * Get the person's profile URL. This URL must be fully qualified. Relative URLs will not work in
   * gadgets. This field MUST be stored in the urls list with a type of "profile".
   *
   * Container support for this field is OPTIONAL.
   *
   * @return the person's profile URL
   */
  String getProfileUrl();

  /**
   * Set the person's profile URL. This URL must be fully qualified. Relative URLs will not work in
   * gadgets. This field MUST be stored in the urls list with a type of "profile".
   *
   * Container support for this field is OPTIONAL.
   *
   * @param profileUrl the person's profile URL
   */
  void setProfileUrl(String profileUrl);

  /**
   * Get the person's photo thumbnail URL, specified as a string. This URL must be fully qualified.
   * Relative URLs will not work in gadgets.
   * This field MUST be stored in the photos list with a type of "thumbnail".
   *
   * Container support for this field is OPTIONAL.
   *
   * @return the person's photo thumbnail URL
   */
  String getThumbnailUrl();

  /**
   * Set the person's photo thumbnail URL, specified as a string. This URL must be fully qualified.
   * Relative URLs will not work in gadgets.
   * This field MUST be stored in the photos list with a type of "thumbnail".
   *
   * Container support for this field is OPTIONAL.
   *
   * @param thumbnailUrl the person's photo thumbnail URL
   */
  void setThumbnailUrl(String thumbnailUrl);
}
