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
package org.apache.shindig.config;

import org.apache.shindig.expressions.Expressions;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.shindig.config.ContainerConfig.DEFAULT_CONTAINER;
import static org.apache.shindig.config.JsonContainerConfig.CONTAINER_KEY;
import static org.apache.shindig.config.JsonContainerConfig.PARENT_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonContainerConfigTest {

  private static final String TOP_LEVEL_NAME = "Top level name";
  private static final String TOP_LEVEL_VALUE = "Top level value";

  private static final String NESTED_KEY = "ne$ted";
  private static final String NESTED_NAME = "Nested name";
  private static final String NESTED_VALUE = "Nested value";
  private static final String NESTED_ALT_VALUE = "Nested value alt";

  private static final String CHILD_CONTAINER = "child";
  private static final String CONTAINER_A = "container-a";
  private static final String CONTAINER_B = "container-b";

  private static final String ARRAY_NAME = "array value";
  private static final String[] ARRAY_VALUE = {"Hello", "World"};
  private static final String ARRAY_ALT_VALUE = "Not an array";

  private File createContainer(JSONObject json) throws Exception {
    File file = File.createTempFile(getClass().getName(), ".json");
    file.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(json.toString());
    out.close();
    return file;
  }

  private File createDefaultContainer() throws Exception {

    // We use a JSON Object here to guarantee that we're well formed up front.
    JSONObject json = new JSONObject();
    json.put(CONTAINER_KEY, new String[]{DEFAULT_CONTAINER});
    json.put(TOP_LEVEL_NAME, TOP_LEVEL_VALUE);
    json.put(ARRAY_NAME, ARRAY_VALUE);

    // small nested data.
    JSONObject nested = new JSONObject();
    nested.put(NESTED_NAME, NESTED_VALUE);

    json.put(NESTED_KEY, nested);
    return createContainer(json);
  }

  @Test
  public void parseBasicConfig() throws Exception {
    ContainerConfig config = new JsonContainerConfig(createDefaultContainer().getAbsolutePath(),
        Expressions.forTesting());

    assertEquals(1, config.getContainers().size());
    for (String container : config.getContainers()) {
      assertEquals(DEFAULT_CONTAINER, container);
    }

    String value = config.getString(DEFAULT_CONTAINER, TOP_LEVEL_NAME);
    assertEquals(TOP_LEVEL_VALUE, value);

    Map<String, Object> nested = config.getMap(DEFAULT_CONTAINER, NESTED_KEY);
    String nestedValue = nested.get(NESTED_NAME).toString();
    assertEquals(NESTED_VALUE, nestedValue);
  }

  @Test
  public void aliasesArePopulated() throws Exception {
    JSONObject json = new JSONObject()
        .put(CONTAINER_KEY, new String[]{CONTAINER_A, CONTAINER_B})
        .put(NESTED_KEY, NESTED_VALUE);

    File parentFile = createDefaultContainer();
    File childFile = createContainer(json);

    ContainerConfig config = new JsonContainerConfig(childFile.getAbsolutePath() +
        JsonContainerConfigLoader.FILE_SEPARATOR + parentFile.getAbsolutePath(), Expressions.forTesting());

    assertEquals(NESTED_VALUE, config.getString(CONTAINER_A, NESTED_KEY));
    assertEquals(NESTED_VALUE, config.getString(CONTAINER_B, NESTED_KEY));
  }

  @Test
  public void parseWithDefaultInheritance() throws Exception {
    JSONObject json = new JSONObject();
    json.put(CONTAINER_KEY, new String[]{CHILD_CONTAINER});
    json.put(PARENT_KEY, DEFAULT_CONTAINER);
    json.put(ARRAY_NAME, ARRAY_ALT_VALUE);

    // small nested data.
    JSONObject nested = new JSONObject();
    nested.put(NESTED_NAME, NESTED_ALT_VALUE);

    json.put(NESTED_KEY, nested);

    File childFile = createContainer(json);
    File parentFile = createDefaultContainer();
    ContainerConfig config = new JsonContainerConfig(childFile.getAbsolutePath() +
        JsonContainerConfigLoader.FILE_SEPARATOR + parentFile.getAbsolutePath(), Expressions.forTesting());

    String value = config.getString(CHILD_CONTAINER, TOP_LEVEL_NAME);
    assertEquals(TOP_LEVEL_VALUE, value);

    Map<String, Object> nestedObj = config.getMap(CHILD_CONTAINER, NESTED_KEY);
    String nestedValue = nestedObj.get(NESTED_NAME).toString();
    assertEquals(NESTED_ALT_VALUE, nestedValue);

    String arrayValue = config.getString(CHILD_CONTAINER, ARRAY_NAME);
    assertEquals(ARRAY_ALT_VALUE, arrayValue);

    // Verify that the parent value wasn't overwritten as well.

    List<String> actual = new ArrayList<String>();
    for (Object val : config.getList(DEFAULT_CONTAINER, ARRAY_NAME)) {
      actual.add(val.toString());
    }

    List<String> expected = Arrays.asList(ARRAY_VALUE);

    assertEquals(expected, actual);
  }

  @Test
  public void invalidContainerReturnsNull() throws Exception {
    ContainerConfig config = new JsonContainerConfig(createDefaultContainer().getAbsolutePath(),
        Expressions.forTesting());
    assertNull("Did not return null for invalid container.", config.getString("fake", PARENT_KEY));
  }

  @Test(expected = ContainerConfigException.class)
  public void badConfigThrows() throws Exception {
    JSONObject json = new JSONObject();
    json.put(CONTAINER_KEY, new String[]{CHILD_CONTAINER});
    json.put(PARENT_KEY, "bad bad bad parent!");
    json.put(ARRAY_NAME, ARRAY_ALT_VALUE);

    new JsonContainerConfig(createContainer(json).getAbsolutePath(), Expressions.forTesting());
  }

  @Test
  public void pathQuery() throws Exception {
    ContainerConfig config = new JsonContainerConfig(createDefaultContainer().getAbsolutePath(), Expressions.forTesting());
    String path = "${" + NESTED_KEY + "['" + NESTED_NAME + "']}";
    String data = config.getString(DEFAULT_CONTAINER, path);
    assertEquals(NESTED_VALUE, data);
  }

  @Test
  public void expressionEvaluation() throws Exception {
    // We use a JSON Object here to guarantee that we're well formed up front.
    JSONObject json = new JSONObject();
    json.put(CONTAINER_KEY, new String[]{DEFAULT_CONTAINER});
    json.put("expression", "Hello, ${world}!");
    json.put("world", "Earth");

    ContainerConfig config = new JsonContainerConfig(createContainer(json).getAbsolutePath(), Expressions.forTesting());

    assertEquals("Hello, Earth!", config.getString(DEFAULT_CONTAINER, "expression"));
  }

  @Test
  public void shindigPortTest() throws Exception {
    // We use a JSON Object here to guarantee that we're well formed up front.
    JSONObject json = new JSONObject();
    json.put(CONTAINER_KEY, new String[]{DEFAULT_CONTAINER});
    json.put("expression", "port=${SERVER_PORT}");

    ContainerConfig config = new JsonContainerConfig(createContainer(json).getAbsolutePath(),
        Expressions.forTesting());

    assertEquals("port=8080", config.getString(DEFAULT_CONTAINER, "expression"));
  }

  @Test
  public void testCommonEnvironmentAddedToAllContainers() throws Exception {
    // We use a JSON Object here to guarantee that we're well formed up front.
    JSONObject json = new JSONObject();
    json.put(CONTAINER_KEY, new String[]{DEFAULT_CONTAINER, "testContainer"});
    json.put("port", "${SERVER_PORT}");
    json.put("host", "${SERVER_HOST}");

    ContainerConfig config = new JsonContainerConfig(createContainer(json).getAbsolutePath(),
        Expressions.forTesting());

    assertEquals("8080", config.getString(DEFAULT_CONTAINER, "port"));
    assertEquals("8080", config.getString("testContainer", "port"));
    assertEquals("localhost", config.getString(DEFAULT_CONTAINER, "host"));
    assertEquals("localhost", config.getString("testContainer", "host"));
  }

  @Test
  public void expressionEvaluationUsingParent() throws Exception {
    // We use a JSON Object here to guarantee that we're well formed up front.
    JSONObject json = new JSONObject();
    json.put(CONTAINER_KEY, new String[]{CHILD_CONTAINER});
    json.put(PARENT_KEY, DEFAULT_CONTAINER);
    json.put("parentExpression", "${parent['" + TOP_LEVEL_NAME + "']}");

    File childFile = createContainer(json);
    File parentFile = createDefaultContainer();
    ContainerConfig config = new JsonContainerConfig(childFile.getAbsolutePath() +
        JsonContainerConfigLoader.FILE_SEPARATOR + parentFile.getAbsolutePath(), Expressions.forTesting());

    assertEquals(TOP_LEVEL_VALUE, config.getString(CHILD_CONTAINER, "parentExpression"));
  }

  @Test
  public void nullEntryEvaluation() throws Exception {
    // We use a JSON Object here to guarantee that we're well formed up front.
    JSONObject json = new JSONObject("{ 'gadgets.container' : ['default'], features : { osapi : null }}");
    JsonContainerConfig config = new JsonContainerConfig(createContainer(json).getAbsolutePath(),
        Expressions.forTesting());
    assertNull(config.getMap("default", "features").get("osapi"));
  }
}
