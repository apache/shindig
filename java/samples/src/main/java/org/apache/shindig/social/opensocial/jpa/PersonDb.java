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
package org.apache.shindig.social.opensocial.jpa;

import static javax.persistence.GenerationType.IDENTITY;

import org.apache.shindig.social.opensocial.model.Account;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Enum;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.opensocial.model.Enum.Drinker;
import org.apache.shindig.social.opensocial.model.Enum.LookingFor;
import org.apache.shindig.social.opensocial.model.Enum.NetworkPresence;
import org.apache.shindig.social.opensocial.model.Enum.Smoker;

import com.google.common.collect.Lists;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REFRESH;
import static javax.persistence.CascadeType.MERGE;

/**
 * Default Implementation of the Person object in the org.apache.shindig.social.opensocial.jpa.
 */
@Entity
@Table(name = "person")
@NamedQueries(value = {
    @NamedQuery(name = PersonDb.FINDBY_PERSONID, query = "select p from PersonDb p where p.id = :id "),
    @NamedQuery(name = PersonDb.FINDBY_LIKE_PERSONID, query = "select p from PersonDb p where p.id like :id") })
public class PersonDb implements Person, DbObject {

  public static final String FINDBY_PERSONID = "q.person.findbypersonid";

  public static final String PARAM_PERSONID = "id";

  public static final String FINDBY_LIKE_PERSONID = "q.person.findbylikepersonid";

  private static final String LOOKING_FOR_PROPERTY = "looking-for";

  private static final String ACTIVITIES_PROPERTY = "activity";

  private static final String BOOKS_PROPERTY = "book";

  private static final String CARS_PROPERTY = "car";

  private static final String HEROES_PROPERTY = "hero";

  private static final String INTERESTS_PROPERTY = "interest";

  private static final String LANGUAGES_PROPERTY = "language";

  private static final String MOVIES_PROPERTY = "movie";

  private static final String MUSIC_PROPERTY = "music";

  private static final String FOOD_PROPERTY = "food";

  private static final String QUOTES_PROPERTY = "quotes";

  private static final String SPORTS_PROPERTY = "sport";

  private static final String TAGS_PROPERTY = "tag";

  private static final String TURNOFFS_PROPERTY = "turnoff";

  private static final String TURNONS_PROPERTY = "turnon";

  private static final String TVSHOWS_PROPERTY = "tvshow";

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "oid")
  private long objectId;

  @Version
  @Column(name = "version")
  protected long version;

  @Basic
  @Column(name = "about_me", length = 255)
  protected String aboutMe;

  @OneToMany(targetEntity = PersonPropertiesDb.class, mappedBy = "person", cascade = ALL)
  protected List<PersonPropertiesDb> properties = new ArrayList<PersonPropertiesDb>();

  @OneToMany(targetEntity = PersonAccountDb.class, mappedBy = "person", cascade = ALL)
  protected List<Account> accounts;

  @Transient
  protected List<String> activities;

  @OneToMany(targetEntity = PersonAddressDb.class, mappedBy = "person", cascade = ALL)
  protected List<Address> addresses;

  @Basic
  @Column(name = "age")
  protected Integer age;

  @ManyToOne(targetEntity = BodyTypeDb.class, cascade = ALL)
  @JoinColumn(name = "body_type_id", referencedColumnName = "oid")
  protected BodyType bodyType;

  @Transient
  protected List<String> books;

  @Transient
  protected List<String> cars;

  @Basic
  @Column(name = "children", length = 255)
  protected String children;

  /**
   * 
   */
  @ManyToOne(targetEntity = AddressDb.class, cascade = { PERSIST, MERGE, REFRESH })
  @JoinColumn(name = "address_id", referencedColumnName = "oid")
  protected Address currentLocation;

  /**
   * 
   */
  @Basic
  @Column(name = "birthday")
  @Temporal(TemporalType.DATE)
  protected Date birthday;

  /**
   * 
   */
  @Basic
  @Column(name = "drinker", length = 255)
  protected String drinkerDb;

  @Transient
  protected Enum<Enum.Drinker> drinker;

  
  @Basic
  @Column(name = "display_name", length = 255)
  private String displayName;

  /**
   * 
   */
  @OneToMany(targetEntity = EmailDb.class, mappedBy = "person", cascade= ALL)
  protected List<ListField> emails;

  /**
   * 
   */
  @Basic
  @Column(name = "ethnicity", length = 255)
  protected String ethnicity;

  /**
   * 
   */
  @Basic
  @Column(name = "fashion", length = 255)
  protected String fashion;

  /**
   * 
   */
  @Transient
  protected List<String> food;

  /**
   * 
   */
  @Basic
  @Column(name = "gender", length = 255)
  protected String genderDb;

  @Transient
  protected Gender gender;

  /**
   * 
   */
  @Basic
  @Column(name = "happiest_when", length = 255)
  protected String happiestWhen;

  /**
   * 
   */
  @Transient
  protected Boolean hasApp;

  /**
   * 
   */
  @Transient
  protected List<String> heroes;

  /**
   * 
   */
  @Basic
  @Column(name = "humor", length = 255)
  protected String humor;

  /**
   * 
   */
  @Basic
  @Column(name = "person_id", length = 255)
  protected String id;

  /**
   * 
   */
  @OneToMany(targetEntity = ImDb.class, mappedBy = "person", cascade = ALL)
  protected List<ListField> ims;

  /**
   * 
   */
  @Transient
  protected List<String> interests;

  /**
   * 
   */
  @Basic
  @Column(name = "job_interests", length = 255)
  protected String jobInterests;

  /**
   * 
   */
  @Transient
  protected List<String> languagesSpoken;

  /**
   * 
   */
  @Basic
  @Column(name = "updated")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date updated;

  /**
   * 
   */
  @Basic
  @Column(name = "living_arrangement", length = 255)
  protected String livingArrangement;

  /**
   * 
   */
  @Transient
  // stored as a property, processed on get,set
  protected List<Enum<Enum.LookingFor>> lookingFor;

  /**
   * 
   */
  @Transient
  protected List<String> movies;

  /**
   * 
   */
  @Transient
  protected List<String> music;

  /**
   * 
   */
  @ManyToOne(targetEntity = NameDb.class, cascade = ALL)
  @JoinColumn(name = "name_id", referencedColumnName = "oid")
  protected Name name;

  /**
   * 
   */
  @Basic
  @Column(name = "network_presence", length = 255)
  protected String networkPresenceDb;

  @Transient
  protected Enum<Enum.NetworkPresence> networkPresence = new EnumDb<NetworkPresence>(NetworkPresence.XA);

  /**
   * 
   */
  @Basic
  @Column(name = "nickname", length = 255)
  protected String nickname;

  /**
   * 
   */
  @OneToMany(targetEntity = PersonOrganizationDb.class, mappedBy = "person", cascade = { PERSIST, MERGE, REFRESH })
  protected List<Organization> organizations;

  /**
   * 
   */
  @Basic
  @Column(name = "pets", length = 255)
  protected String pets;

  /**
   * 
   */
  @OneToMany(targetEntity = PhoneDb.class, mappedBy = "person", cascade = ALL)
  protected List<ListField> phoneNumbers;

  /**
   * 
   */
  @OneToMany(targetEntity = PhotoDb.class, mappedBy = "person", cascade = ALL)
  protected List<ListField> photos;
  @Basic
  @Column(name = "political_views", length = 255)
  protected String politicalViews;

  /**
   * 
   */
  @Transient
  protected Url profileSong;

  /**
   * 
   */
  @Transient
  protected Url profileVideo;

  /**
   * 
   */
  @Transient
  protected List<String> quotes;

  /**
   * 
   */
  @Basic
  @Column(name = "relationship_status", length = 255)
  protected String relationshipStatus;

  /**
   * 
   */
  @Basic
  @Column(name = "religion", length = 255)
  protected String religion;

  /**
   * 
   */
  @Basic
  @Column(name = "romance", length = 255)
  protected String romance;

  /**
   * 
   */
  @Basic
  @Column(name = "scared_of", length = 255)
  protected String scaredOf;

  /**
   * 
   */
  @Basic
  @Column(name = "sexual_orientation", length = 255)
  protected String sexualOrientation;

  /**
   * 
   */
  @Basic
  @Column(name = "smoker", length = 255)
  protected String smokerDb;

  @Transient
  protected Enum<Enum.Smoker> smoker;

  /**
   * 
   */
  @Transient
  protected List<String> sports;

  /**
   * 
   */
  @Basic
  @Column(name = "status", length = 255)
  protected String status;

  /**
   * 
   */
  @Transient
  protected List<String> tags;

  /**
   * 
   */
  @Basic
  @Column(name = "utc_offset")
  protected Long utcOffset;

  /**
   * 
   */
  @Transient
  protected List<String> turnOffs;

  /**
   * 
   */
  @Transient
  protected List<String> turnOns;

  /**
   * 
   */
  @Transient
  protected List<String> tvShows;

  /**
   * 
   */
  @OneToMany(targetEntity = UrlDb.class, mappedBy = "person", cascade = ALL)
  protected List<Url> urls;

  // Note: Not in the opensocial js person object directly
  @Transient
  private boolean isOwner = false;

  @Transient
  private boolean isViewer = false;


  public PersonDb() {
  }

  public PersonDb(String id, String displayName , Name name) {
    this.id = id;
    this.name = name;
    this.displayName = displayName;
  }

  public String getAboutMe() {
    return aboutMe;
  }

  public void setAboutMe(String aboutMe) {
    this.aboutMe = aboutMe;
  }

  public List<Account> getAccounts() {
    return accounts;
  }

  public void setAccounts(List<Account> accounts) {
    this.accounts = accounts;
  }

  public List<String> getActivities() {
    return activities;
  }

  public void setActivities(List<String> activities) {
    this.activities = activities;
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses = addresses;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public BodyType getBodyType() {
    return bodyType;
  }

  public void setBodyType(BodyType bodyType) {
    this.bodyType = bodyType;
  }

  public List<String> getBooks() {
    return books;
  }

  public void setBooks(List<String> books) {
    this.books = books;
  }

  public List<String> getCars() {
    return cars;
  }

  public void setCars(List<String> cars) {
    this.cars = cars;
  }

  public String getChildren() {
    return children;
  }

  public void setChildren(String children) {
    this.children = children;
  }

  public Address getCurrentLocation() {
    return currentLocation;
  }

  public void setCurrentLocation(Address currentLocation) {
    this.currentLocation = currentLocation;
  }

  public Date getBirthday() {
    if (birthday == null) {
      return null;
    }
    return new Date(birthday.getTime());
  }

  public void setBirthday(Date birthday) {
    if (birthday == null) {
      this.birthday = null;
    } else {
      this.birthday = new Date(birthday.getTime());
    }
  }

  public Enum<Enum.Drinker> getDrinker() {
    return this.drinker;
  }

  public void setDrinker(Enum<Enum.Drinker> newDrinker) {
    this.drinker = newDrinker;
  }

  public List<ListField> getEmails() {
    return emails;
  }

  public void setEmails(List<ListField> emails) {
    this.emails = emails;
  }

  public String getEthnicity() {
    return ethnicity;
  }

  public void setEthnicity(String ethnicity) {
    this.ethnicity = ethnicity;
  }

  public String getFashion() {
    return fashion;
  }

  public void setFashion(String fashion) {
    this.fashion = fashion;
  }

  public List<String> getFood() {
    return food;
  }

  public void setFood(List<String> food) {
    this.food = food;
  }

  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender newGender) {
    this.gender = newGender;
  }

  public String getHappiestWhen() {
    return happiestWhen;
  }

  public void setHappiestWhen(String happiestWhen) {
    this.happiestWhen = happiestWhen;
  }

  public Boolean getHasApp() {
    return hasApp;
  }

  public void setHasApp(Boolean hasApp) {
    this.hasApp = hasApp;
  }

  public List<String> getHeroes() {
    return heroes;
  }

  public void setHeroes(List<String> heroes) {
    this.heroes = heroes;
  }

  public String getHumor() {
    return humor;
  }

  public void setHumor(String humor) {
    this.humor = humor;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<ListField> getIms() {
    return ims;
  }

  public void setIms(List<ListField> ims) {
    this.ims = ims;
  }

  public List<String> getInterests() {
    return interests;
  }

  public void setInterests(List<String> interests) {
    this.interests = interests;
  }

  public String getJobInterests() {
    return jobInterests;
  }

  public void setJobInterests(String jobInterests) {
    this.jobInterests = jobInterests;
  }

  public List<String> getLanguagesSpoken() {
    return languagesSpoken;
  }

  public void setLanguagesSpoken(List<String> languagesSpoken) {
    this.languagesSpoken = languagesSpoken;
  }

  public Date getUpdated() {
    if (updated == null) {
      return null;
    }
    return new Date(updated.getTime());
  }

  public void setUpdated(Date updated) {
    if (updated == null) {
      this.updated = null;
    } else {
      this.updated = new Date(updated.getTime());
    }
  }

  public String getLivingArrangement() {
    return livingArrangement;
  }

  public void setLivingArrangement(String livingArrangement) {
    this.livingArrangement = livingArrangement;
  }

  public List<Enum<Enum.LookingFor>> getLookingFor() {
    return lookingFor;
  }

  public void setLookingFor(List<Enum<Enum.LookingFor>> lookingFor) {
    this.lookingFor = lookingFor;
  }

  public List<String> getMovies() {
    return movies;
  }

  public void setMovies(List<String> movies) {
    this.movies = movies;
  }

  public List<String> getMusic() {
    return music;
  }

  public void setMusic(List<String> music) {
    this.music = music;
  }

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public Enum<Enum.NetworkPresence> getNetworkPresence() {
    return networkPresence;
  }

  public void setNetworkPresence(Enum<Enum.NetworkPresence> networkPresence) {
    this.networkPresence = networkPresence;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public List<Organization> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(List<Organization> organizations) {
    this.organizations = organizations;
  }

  public String getPets() {
    return pets;
  }

  public void setPets(String pets) {
    this.pets = pets;
  }

  public List<ListField> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(List<ListField> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  public List<ListField> getPhotos() {
    return photos;
  }

  public void setPhotos(List<ListField> photos) {
    this.photos = photos;
  }

  public String getPoliticalViews() {
    return politicalViews;
  }

  public void setPoliticalViews(String politicalViews) {
    this.politicalViews = politicalViews;
  }

  public Url getProfileSong() {
    return profileSong;
  }

  public void setProfileSong(Url profileSong) {
    this.profileSong = profileSong;
  }

  public Url getProfileVideo() {
    return profileVideo;
  }

  public void setProfileVideo(Url profileVideo) {
    this.profileVideo = profileVideo;
  }

  public List<String> getQuotes() {
    return quotes;
  }

  public void setQuotes(List<String> quotes) {
    this.quotes = quotes;
  }

  public String getRelationshipStatus() {
    return relationshipStatus;
  }

  public void setRelationshipStatus(String relationshipStatus) {
    this.relationshipStatus = relationshipStatus;
  }

  public String getReligion() {
    return religion;
  }

  public void setReligion(String religion) {
    this.religion = religion;
  }

  public String getRomance() {
    return romance;
  }

  public void setRomance(String romance) {
    this.romance = romance;
  }

  public String getScaredOf() {
    return scaredOf;
  }

  public void setScaredOf(String scaredOf) {
    this.scaredOf = scaredOf;
  }

  public String getSexualOrientation() {
    return sexualOrientation;
  }

  public void setSexualOrientation(String sexualOrientation) {
    this.sexualOrientation = sexualOrientation;
  }

  public Enum<Enum.Smoker> getSmoker() {
    return this.smoker;
  }

  public void setSmoker(Enum<Enum.Smoker> newSmoker) {
    this.smoker = newSmoker;
  }

  public List<String> getSports() {
    return sports;
  }

  public void setSports(List<String> sports) {
    this.sports = sports;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public Long getUtcOffset() {
    return utcOffset;
  }

  public void setUtcOffset(Long utcOffset) {
    this.utcOffset = utcOffset;
  }

  public List<String> getTurnOffs() {
    return turnOffs;
  }

  public void setTurnOffs(List<String> turnOffs) {
    this.turnOffs = turnOffs;
  }

  public List<String> getTurnOns() {
    return turnOns;
  }

  public void setTurnOns(List<String> turnOns) {
    this.turnOns = turnOns;
  }

  public List<String> getTvShows() {
    return tvShows;
  }

  public void setTvShows(List<String> tvShows) {
    this.tvShows = tvShows;
  }

  public List<Url> getUrls() {
    return urls;
  }

  public void setUrls(List<Url> urls) {
    this.urls = urls;
  }

  public boolean getIsOwner() {
    return isOwner;
  }

  public void setIsOwner(boolean isOwner) {
    this.isOwner = isOwner;
  }

  public boolean getIsViewer() {
    return isViewer;
  }

  public void setIsViewer(boolean isViewer) {
    this.isViewer = isViewer;
  }

  // Proxied fields

  public String getProfileUrl() {
    Url url = getListFieldWithType(PROFILE_URL_TYPE, getUrls());
    return url == null ? null : url.getValue();
  }

  public void setProfileUrl(String profileUrl) {
    Url url = getListFieldWithType(PROFILE_URL_TYPE, getUrls());
    if (url != null) {
      url.setValue(profileUrl);
    } else {
      setUrls(addListField(new UrlDb(profileUrl, null, PROFILE_URL_TYPE), getUrls()));
    }
  }

  public String getThumbnailUrl() {
    ListField photo = getListFieldWithType(THUMBNAIL_PHOTO_TYPE, getPhotos());
    return photo == null ? null : photo.getValue();
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    ListField photo = getListFieldWithType(THUMBNAIL_PHOTO_TYPE, getPhotos());
    if (photo != null) {
      photo.setValue(thumbnailUrl);
    } else {
      setPhotos(addListField(new ListFieldDb(THUMBNAIL_PHOTO_TYPE, thumbnailUrl), getPhotos()));
    }
  }

  private <T extends ListField> T getListFieldWithType(String type, List<T> list) {
    if (list != null) {
      for (T url : list) {
        if (type.equalsIgnoreCase(url.getType())) {
          return url;
        }
      }
    }

    return null;
  }

  private <T extends ListField> List<T> addListField(T field, List<T> list) {
    if (list == null) {
      list = Lists.newArrayList();
    }
    list.add(field);
    return list;
  }

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(long objectId) {
    this.objectId = objectId;
  }

  @PrePersist
  public void populateDbFields() {
    drinkerDb = drinker.toString();
    genderDb = gender.toString();
    networkPresenceDb = networkPresence.toString();
    smokerDb = smoker.toString();

    List<String> lookingFor = new ArrayList<String>();
    for (Enum<Enum.LookingFor> np : this.lookingFor) {
      lookingFor.add(np.toString());
    }
    Map<String, List<String>> toSave = new HashMap<String, List<String>>();
    toSave.put(LOOKING_FOR_PROPERTY, lookingFor);
    toSave.put(ACTIVITIES_PROPERTY, this.activities);
    toSave.put(BOOKS_PROPERTY, this.books);
    toSave.put(CARS_PROPERTY, this.cars);
    toSave.put(FOOD_PROPERTY, this.food);
    toSave.put(HEROES_PROPERTY, this.heroes);
    toSave.put(INTERESTS_PROPERTY, this.interests);
    toSave.put(LANGUAGES_PROPERTY, this.languagesSpoken);
    toSave.put(MOVIES_PROPERTY, this.movies);
    toSave.put(MUSIC_PROPERTY, this.music);
    toSave.put(QUOTES_PROPERTY, this.quotes);
    toSave.put(SPORTS_PROPERTY, this.sports);
    toSave.put(TAGS_PROPERTY, this.tags);
    toSave.put(TURNOFFS_PROPERTY, this.turnOffs);
    toSave.put(TURNONS_PROPERTY, this.turnOns);
    toSave.put(TVSHOWS_PROPERTY, this.tvShows);

    for (Entry<String, List<String>> e : toSave.entrySet()) {
      // add new entries
      for (String v : e.getValue()) {
        boolean present = false;
        for (PersonPropertiesDb pp : properties) {
          if (e.getKey().equals(pp.getType()) && v.equals(pp.getValue())) {
            present = true;
            break;
          }
        }
        if (!present) {
          PersonPropertiesDb pp = new PersonPropertiesDb();
          pp.setType(e.getKey());
          pp.setValue(v);
        }
      }
      // remove missing entries
      List<PersonPropertiesDb> toRemove = new ArrayList<PersonPropertiesDb>();
      for (PersonPropertiesDb pp : properties) {
        if (e.getKey().equals(pp.getType())) {
          boolean present = false;
          for (String v : e.getValue()) {
            if (pp.getValue().equals(v)) {
              present = true;
              break;
            }
          }
          if (!present) {
            toRemove.add(pp);
          }
        }
      }
      properties.removeAll(toRemove);
    }
  }

  @PostLoad
  public void loadTransientFields() {

    drinkerDb = drinker.toString();
    genderDb = gender.toString();
    networkPresenceDb = networkPresence.toString();
    smokerDb = smoker.toString();

    drinker = new EnumDb<Drinker>(Drinker.valueOf(drinkerDb));
    gender = Gender.valueOf(genderDb);
    networkPresence = new EnumDb<NetworkPresence>(NetworkPresence.valueOf(networkPresenceDb));
    smoker = new EnumDb<Smoker>(Smoker.valueOf(smokerDb));

    List<String> lookingFor = new ArrayList<String>();
    this.activities = new ArrayList<String>();
    this.books = new ArrayList<String>();
    this.cars = new ArrayList<String>();
    this.food = new ArrayList<String>();
    this.heroes = new ArrayList<String>();
    this.interests = new ArrayList<String>();
    this.languagesSpoken = new ArrayList<String>();
    this.movies = new ArrayList<String>();
    this.music = new ArrayList<String>();
    this.quotes = new ArrayList<String>();
    this.sports = new ArrayList<String>();
    this.tags = new ArrayList<String>();
    this.turnOffs = new ArrayList<String>();
    this.turnOns = new ArrayList<String>();
    this.tvShows = new ArrayList<String>();

    Map<String, List<String>> toSave = new HashMap<String, List<String>>();

    toSave.put(LOOKING_FOR_PROPERTY, lookingFor);
    toSave.put(ACTIVITIES_PROPERTY, this.activities);
    toSave.put(BOOKS_PROPERTY, this.books);
    toSave.put(CARS_PROPERTY, this.cars);
    toSave.put(FOOD_PROPERTY, this.food);
    toSave.put(HEROES_PROPERTY, this.heroes);
    toSave.put(INTERESTS_PROPERTY, this.interests);
    toSave.put(LANGUAGES_PROPERTY, this.languagesSpoken);
    toSave.put(MOVIES_PROPERTY, this.movies);
    toSave.put(MUSIC_PROPERTY, this.music);
    toSave.put(QUOTES_PROPERTY, this.quotes);
    toSave.put(SPORTS_PROPERTY, this.sports);
    toSave.put(TAGS_PROPERTY, this.tags);
    toSave.put(TURNOFFS_PROPERTY, this.turnOffs);
    toSave.put(TURNONS_PROPERTY, this.turnOns);
    toSave.put(TVSHOWS_PROPERTY, this.tvShows);

    for (PersonPropertiesDb pp : properties) {
      List<String> l = toSave.get(pp.type);
      if (l != null) {
        l.add(pp.getValue());
      }
    }

    this.lookingFor = new ArrayList<Enum<LookingFor>>();
    for (String lf : lookingFor) {
      this.lookingFor.add(new EnumDb<LookingFor>(LookingFor.valueOf(lf)));
    }

  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.model.Person#getDisplayName()
   */
  public String getDisplayName() {
    return displayName;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.model.Person#setDisplayName(java.lang.String)
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;    
  }
}
