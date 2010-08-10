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

package org.apache.shindig.social.opensocial.jpa.test;

import com.google.common.collect.Lists;

import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.EnumImpl;
import org.apache.shindig.social.opensocial.jpa.AddressDb;
import org.apache.shindig.social.opensocial.jpa.BodyTypeDb;
import org.apache.shindig.social.opensocial.jpa.EmailDb;
import org.apache.shindig.social.opensocial.jpa.NameDb;
import org.apache.shindig.social.opensocial.jpa.OrganizationAddressDb;
import org.apache.shindig.social.opensocial.jpa.OrganizationDb;
import org.apache.shindig.social.opensocial.jpa.PersonAddressDb;
import org.apache.shindig.social.opensocial.jpa.PersonDb;
import org.apache.shindig.social.opensocial.jpa.PersonOrganizationDb;
import org.apache.shindig.social.opensocial.jpa.PhoneDb;
import org.apache.shindig.social.opensocial.jpa.PhotoDb;
import org.apache.shindig.social.opensocial.jpa.UrlDb;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Drinker;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.LookingFor;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.opensocial.model.Person.Gender;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class PersonPopulate {

  private EntityManager entityManager;

  /**
   *
   */
  public PersonPopulate(EntityManager entityManager) {
    this.entityManager = entityManager;
    // TODO Auto-generated constructor stub
  }

  private static final Logger LOG = Logger.getLogger("shindig-db-test");

  public Person createPerson(int i, long key, Random random) {

    Person person = new PersonDb();
    person.setAboutMe("About Me " + i);
    String personId = getPersonId(i, key);

    person.setId(personId);
    person.setActivities(getList("Activities"));
    int age = random.nextInt(105);
    Calendar c = new GregorianCalendar();
    c.setTimeInMillis(System.currentTimeMillis());
    c.add(Calendar.YEAR, -age);
    c.add(Calendar.MONTH, 12 - i % 12);
    List<Address> a = Lists.newArrayList();
    a.add(getNewPersonAddress(i));
    a.add(getNewPersonAddress(i + 2));
    person.setAddresses(a);
    person.setAge(random.nextInt(105));
    person.setBodyType(getNewBodyType(i));
    person.setBooks(getList("Books"));
    person.setCars(getList("Cars"));
    person.setChildren("Yes");
    person.setCurrentLocation(getNewAddress(i + 5));
    person.setBirthday(c.getTime());
    person.setDrinker(new EnumImpl<Drinker>(Drinker.OCCASIONALLY));
    List<ListField> emails = Lists.newArrayList();
    emails.add(getNewEmail(i));
    emails.add(getNewEmail(i + 1));
    person.setEmails(emails);
    person.setEthnicity("ethinicity");
    person.setFashion("fashion");
    person.setFood(getList("Food"));
    person.setGender(Gender.female);
    person.setHappiestWhen("sailing");
    person.setHeroes(getList("Heroes"));
    person.setHumor("hahaha");
    person.setInterests(getList("Interests"));
    person.setIsOwner(true);
    person.setIsViewer(true);
    person.setJobInterests("job interest");
    List<Organization> organizations = Lists.newArrayList();
    organizations.add(getPersonOrganization(i, "job"));
    organizations.add(getPersonOrganization(i + 1, "job"));
    organizations.add(getPersonOrganization(i + 2, "job"));
    person.setOrganizations(organizations);
    person.setLanguagesSpoken(getList("LanguagesSpoken"));
    person.setLivingArrangement("living Arrangement");
    List<Enum<LookingFor>> lookingFor = Lists.newArrayList();
    Enum<LookingFor> lookingForOne = new EnumImpl<LookingFor>(LookingFor.RANDOM);
    Enum<LookingFor> lookingForTwo = new EnumImpl<LookingFor>(LookingFor.NETWORKING);
    lookingFor.add(lookingForOne);
    lookingFor.add(lookingForTwo);
    person.setLookingFor(lookingFor);
    person.setMovies(getList("Movies"));
    person.setMusic(getList("music"));
    person.setName(getNewName(i));
    person.setNickname("NickName");
    person.setPets("Pets");
    List<ListField> phoneNumbers = Lists.newArrayList();
    phoneNumbers.add(getNewPhone(i));
    phoneNumbers.add(getNewPhone(i * 3));

    person.setPhoneNumbers(phoneNumbers);
    person.setPoliticalViews("politicalViews");
    person.setProfileSong(getNewUrl(i));
    person.setProfileUrl("Profile URL");
    person.setProfileVideo(getNewUrl(i * 2));
    person.setQuotes(getList("Quites"));
    person.setRelationshipStatus("relationship");
    person.setReligion("religion");
    person.setRomance("romance");
    person.setScaredOf("scaredOf");
    List<Organization> organizations2 = person.getOrganizations();
    organizations2.add(getPersonOrganization(i + 5, "school"));
    organizations2.add(getPersonOrganization(i + 6, "school"));
    organizations2.add(getPersonOrganization(i + 7, "school"));
    person.setOrganizations(organizations2);
    person.setSexualOrientation("sexualOrientation");
    person.setSmoker(new EnumImpl<Smoker>(Smoker.QUITTING));
    person.setSports(getList("Sports"));
    person.setStatus("Status");
    person.setTags(getList("tags"));

    List<ListField> photos = Lists.newArrayList();
    photos.add(getNewPhoto(i));
    photos.add(getNewPhoto(i * 3));

    person.setPhotos(photos);
    person.setUtcOffset(1L);
    person.setTurnOffs(getList("TurnOff"));
    person.setTurnOns(getList("TurnOns"));
    person.setTvShows(getList("TvShows"));
    person.setUpdated(new Date());
    List<Url> urls = Lists.newArrayList();
    urls.add(getNewUrl(i * 4));
    urls.add(getNewUrl(i * 5));
    urls.add(getNewUrl(i * 6));
    person.setUrls(urls);

    // TODO: setActivity
    // TODO: person.setAccounts(accounts);
    // TODO: person.setActivities(activities);
    // TODO: person.setAddresses(addresses);

    LOG.info("Created user ++++++ " + personId);

    return person;
  }

  public String getPersonId(int i, long key) {
    return "Person" + key + ':' + i;
  }

  private Url getNewUrl(int i) {
    String targetUrl = "http://sdfsdfsd.sdfdsf/" + String.valueOf(i % 33);
    List<?> l = find(UrlDb.FINDBY_URL,
        new String[] { UrlDb.PARAM_URL }, new Object[] { targetUrl });
    if (l.isEmpty()) {
      Url url = new UrlDb();
      url.setValue(targetUrl);
      url.setLinkText("LinkText");
      url.setType("URL");
      return url;
    } else {
      return (Url) l.get(0);
    }
  }

  private PhoneDb getNewPhone(int i) {
    String targetPhone = String.valueOf(i % 33);
    PhoneDb phone = findOne(PhoneDb.FINDBY_PHONE_NUMBER,
        new String[] { PhoneDb.PARAM_PHONE_NUMBER }, new Object[] { targetPhone });
    if (phone == null) {
      phone = new PhoneDb();
      phone.setValue(targetPhone);
      phone.setType("Mobile");
    }
    return phone;
  }

  private PhotoDb getNewPhoto(int i) {
    String targetPhoto = String.valueOf(i % 33);
    PhotoDb photo = findOne(PhotoDb.FINDBY_PHOTO,
        new String[] { PhotoDb.PARAM_PHOTO }, new Object[] { targetPhoto });
    if (photo == null) {
      photo = new PhotoDb();
      photo.setValue(targetPhoto);
      photo.setType("Mobile");
    }
    return photo;
  }

  private Name getNewName(int i) {
    String targetName = String.valueOf("FamilyName" + (i % 25));
    Name name = findOne(NameDb.FINDBY_FAMILY_NAME, new String[] { NameDb.PARAM_FAMILY_NAME },
        new Object[] { targetName });
    if (name == null) {
      name = new NameDb();
      name.setFamilyName(targetName);
      name.setGivenName("GivenName");
      name.setHonorificPrefix("Hprefix");
      name.setHonorificSuffix("HSufix");
      name.setFormatted("formatted");
      name.setAdditionalName("Additional Names");
    }
    return name;
  }

  private List<String> getList(String base) {
    List<String> list = Lists.newArrayList();
    for (int i = 0; i < 10; i++) {
      list.add(base + i);
    }
    return list;
  }

  @SuppressWarnings("unused")
  private Organization getDbOrganization(int i, String type) {

    String targetOrg = "Organization_" + (i % 10);
    Organization organization = findOne(OrganizationDb.FINDBY_NAME,
        new String[] { OrganizationDb.PARAM_NAME }, new Object[] { targetOrg });

    if (organization == null) {
      organization = new OrganizationDb();
      organization.setAddress(getNewOrganizationAddress(i * 3));
      organization.setName(targetOrg);
      organization.setSubField("SubField");
      organization.setTitle("Title");
      organization.setWebpage("http://sdfsd.sdfsdf.sdfsdf");
    }
    return organization;
  }

  private Organization getPersonOrganization(int i, String type) {
    String targetOrg = "Organization_" + (i % 10);
    PersonOrganizationDb organization = findOne(PersonOrganizationDb.PERSON_ORG_FINDBY_NAME,
        new String[] { PersonOrganizationDb.PARAM_NAME }, new Object[] { targetOrg });
    if (organization == null) {
      organization = new PersonOrganizationDb();
      organization.setDescription("Description");
      organization.setEndDate(new Date(System.currentTimeMillis()
          + (24L * 3600L * 1000L * 365L * 2L)));
      organization.setAddress(getNewOrganizationAddress(i * 3));
      organization.setName(targetOrg);
      organization.setSalary(String.valueOf(i * 1000));
      organization.setStartDate(new Date(System.currentTimeMillis()
          - (24L * 3600L * 1000L * 365L * 2L)));
      organization.setField("Field");
      organization.setSubField("SubField");
      organization.setTitle("Title");
      organization.setType(type);
      organization.setWebpage("http://sdfsd.sdfsdf.sdfsdf");
    }
    return organization;

  }

  @SuppressWarnings("unchecked")
  private <T> T find(String query, String[] names, Object[] params) {
    Query q = entityManager.createNamedQuery(query);
    for (int i = 0; i < names.length; i++) {
      q.setParameter(names[i], params[i]);
    }
    return (T) q.getResultList();
  }

  private <T> T findOne(String query, String[] names, Object[] params) {
    List<T> l = find(query, names, params);
    if (!l.isEmpty()) {
      return l.get(0);
    }
    return null;
  }

  private ListField getNewEmail(int i) {
    String targetAddress = "xyz" + i + "@testdataset.com";
    ListField email = findOne(EmailDb.FINDBY_EMAIL, new String[] { EmailDb.PARAM_EMAIL },
        new Object[] { targetAddress });
    if (email == null) {
      email = new EmailDb();
      email.setValue(targetAddress);
      email.setType("emailType");
    }
    return email;
  }

  private BodyType getNewBodyType(int i) {
    BodyType bodyType = findOne(BodyTypeDb.FINDBY_HEIGHT, new String[] { BodyTypeDb.PARAM_HEIGHT },
        new Object[] { new Float(i % 10) });
    if (bodyType == null) {
      bodyType = new BodyTypeDb();
      bodyType.setBuild("Build " + i);
      bodyType.setEyeColor("Build " + i);
      bodyType.setHairColor("Build " + i);
      bodyType.setHeight(new Float(i % 10));
      bodyType.setWeight(new Float(i % 15));
    }
    return bodyType;
  }

  private Address getNewAddress(int i) {
    Address address = findOne(AddressDb.FINDBY_POSTCODE, new String[] { AddressDb.PARAM_POSTCODE },
        new Object[] { String.valueOf(i % 10) });
    if (address == null) {
      address = new AddressDb();
      address.setCountry("UK");
      address.setLatitude(new Float(0.5));
      address.setLongitude(new Float(0.0));
      address.setPostalCode(String.valueOf(i % 10));
      address.setRegion("CAMBS");
      address.setStreetAddress("High Street");
      address.setType("sometype:");
      address.setFormatted("formatted address");
      address.setLocality("locality");
      address.setPrimary(false);
      address.setType("home");
    }
    return address;
  }

  private Address getNewOrganizationAddress(int i) {
    Address address = findOne(AddressDb.FINDBY_POSTCODE, new String[] { AddressDb.PARAM_POSTCODE },
        new Object[] { String.valueOf(i % 10) });
    if (address == null) {
      address = new OrganizationAddressDb();
      address.setCountry("UK");
      address.setLatitude(new Float(0.5));
      address.setLongitude(new Float(0.0));
      address.setPostalCode(String.valueOf(i % 10));
      address.setRegion("CAMBS");
      address.setStreetAddress("High Street");
      address.setType("sometype:");
      address.setFormatted("formatted address");
      address.setLocality("locality");
      address.setPrimary(false);
      address.setType("home");
    }
    return address;
  }

  private Address getNewPersonAddress(int i) {
    Address address = findOne(AddressDb.FINDBY_POSTCODE, new String[] { AddressDb.PARAM_POSTCODE },
        new Object[] { String.valueOf(i % 10) });
    if (address == null) {
      address = new PersonAddressDb();
      address.setCountry("UK");
      address.setLatitude(new Float(0.5));
      address.setLongitude(new Float(0.0));
      address.setPostalCode(String.valueOf(i % 10));
      address.setRegion("CAMBS");
      address.setStreetAddress("High Street");
      address.setType("sometype:");
      address.setFormatted("formatted address");
      address.setLocality("locality");
      address.setPrimary(false);
      address.setType("home");
    }
    return address;
  }

  public void destroyPerson(int i, long key) {
    List<Person> people = find(PersonDb.FINDBY_LIKE_PERSONID,
        new String[] { PersonDb.PARAM_PERSONID }, new Object[] { getPersonId(i, key) });
    for (Person o : people) {
      entityManager.remove(o);
    }
  }

  protected Person getPerson(String id) {
    return find(PersonDb.FINDBY_PERSONID, new String[] { PersonDb.PARAM_PERSONID },
        new Object[] { id });
  }

}
