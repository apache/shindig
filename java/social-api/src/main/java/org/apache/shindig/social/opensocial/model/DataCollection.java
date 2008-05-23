package org.apache.shindig.social.opensocial.model;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class DataCollection implements Iterable<DataCollection.Data> {
  Set<Data> data;

  // Legacy constructor. Remove once we fix the DataService api
  public DataCollection(Map<String, Map<String, String>> data) {
    this.data = new HashSet<Data>();
    for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
      this.data.add(new Data(entry.getKey(), entry.getValue()));
    }
  }

  public DataCollection(Set<Data> data) {
    this.data = data;
  }

  public Set<Data> getData() {
    return data;
  }

  public Iterator<Data> iterator() {
    return data.iterator();
  }

  public Map<String, Map<String, String>> dataMap() {
    Map<String, Map<String, String>> dataMap = new HashMap<String,
        Map<String, String>>();
    for (Data entry : data) {
      dataMap.put(entry.getPersonId(), entry.getAppdata());
    }
    return dataMap;
  }

  public static class Data {
    String personId;
    Map<String, String> appdata;

    public Data(String personId, Map<String, String> appdata) {
      this.personId = personId;
      this.appdata = appdata;
    }

    public String getPersonId() {
      return personId;
    }

    public Map<String, String> getAppdata() {
      return appdata;
    }
  }
}
