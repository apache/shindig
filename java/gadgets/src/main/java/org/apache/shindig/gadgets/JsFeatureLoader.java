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
package org.apache.shindig.gadgets;

import org.apache.shindig.util.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Provides a mechanism for loading a group of js features from a directory.
 *
 * All directories from the given input will be checked recursively for files
 * named "feature.xml"
 *
 * Usage:
 * GadgetFeatureRegistry registry = // get your feature registry.
 * JsFeatureLoader loader = new JsFeatureLoader();
 * loader.loadFeatures("res://features/", registry);
 * loader.loadFeatures("/home/user/my-features/", registry);
 */
public class JsFeatureLoader {

  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  /**
   * Loads all of the gadgets in the directory specified by path. Invalid
   * features will not cause this to fail, but passing an invalid path will.
   *
   * @param path The file or directory to load the feature from. If feature.xml
   *    is passed in directly, it will be loaded as a single feature. If a
   *    directory is passed, any features in that directory (recursively) will
   *    be loaded. If res://*.txt is passed, we will look for named resources
   *    in the text file. If path is prefixed with res://, the file
   *    is treated as a resource, and all references are assumed to be
   *    resources as well.
   * @return A list of the newly loaded features.
   * @throws GadgetException
   */
  public List<GadgetFeatureRegistry.Entry> loadFeatures(String path,
      GadgetFeatureRegistry registry) throws GadgetException {
    Map<String, ParsedFeature> deps = new HashMap<String, ParsedFeature>();
    try {
      if (path.startsWith("res://")) {
        path = path.substring(6);
        logger.info("Loading resources from: " + path);
        if (path.endsWith(".txt")) {
          loadResources(ResourceLoader.getContent(path).split("\n"), deps);
        } else {
          loadResources(new String[]{path}, deps);
        }
      } else {
        logger.info("Loading files from: " + path);
        File file = new File(path);
        loadFiles(new File[]{file}, deps);
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    }

    List<GadgetFeatureRegistry.Entry> entries
        = new LinkedList<GadgetFeatureRegistry.Entry>();

    // This ensures that we register everything in the right order.
    Set<String> registered = new HashSet<String>();
    for (Map.Entry<String, ParsedFeature> entry : deps.entrySet()) {
      ParsedFeature feature = entry.getValue();
      GadgetFeatureRegistry.Entry feat
          = register(registry, feature, registered, deps);
      if (feat != null) {
        entries.add(feat);
      }
    }
    return entries;
  }

  /**
   * Loads features from directories recursively.
   * @param files The files to examine.
   * @param features The set of all loaded features
   * @throws GadgetException
   */
  private void loadFiles(File[] files, Map<String, ParsedFeature> features)
      throws GadgetException {
    for (File file : files) {
      if (file.isDirectory()) {
        loadFiles(file.listFiles(), features);
      } else if (file.getName().endsWith(".xml")) {
        ParsedFeature feature = processFile(file);
        if (feature != null) {
          features.put(feature.name, feature);
        }
      }
    }
  }

  /**
   * Loads resources recursively.
   * @param paths The base paths to look for feature.xml
   * @param feats The set of all loaded features
   * @throws GadgetException
   */
  private void loadResources(String[] paths, Map<String, ParsedFeature> feats)
      throws GadgetException {
    try {
      for (String file : paths) {
        String content = ResourceLoader.getContent(file);
        String parent = file.substring(0, file.lastIndexOf('/') + 1);
        ParsedFeature feature = parse(content, parent, true);
        if (feature != null) {
          feats.put(feature.name, feature);
        }
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    }
  }

  /**
   * Loads a single feature from a file.
   *
   * If the file can't be loaded, an error will be generated but no exception
   * will be thrown.
   *
   * @param file The file that contains the feature description.
   * @return The parsed feature.
   */
  private ParsedFeature processFile(File file) {
    logger.info("Loading file: " + file.getName());
    ParsedFeature feature = null;
    if (file.canRead()) {
      try {
        feature = parse(ResourceLoader.getContent(file),
                        file.getParent() + '/',
                        false);
      } catch (IOException e) {
        logger.warning("Error reading file: " + file.getAbsolutePath());
      } catch (GadgetException e) {
        logger.warning("Failed parsing file: " + file.getAbsolutePath());
      }
    } else {
      logger.warning("Unable to read file: " + file.getAbsolutePath());
    }
    return feature;
  }

  /**
   * Registers a feature and ensures that dependencies are registered in the
   * proper order.
   *
   * @param registry The registry to store the newly registered features to.
   * @param feature The feature to register.
   * @param registered Set of all features registered during this operation.
   * @param all Map of all features that can be loaded during this operation.
   */
  private GadgetFeatureRegistry.Entry register(GadgetFeatureRegistry registry,
                                               ParsedFeature feature,
                                               Set<String> registered,
                                               Map<String, ParsedFeature> all) {
    if (registered.contains(feature.name)) {
      return null;
    }

    for (String dep : feature.deps) {
      if (all.containsKey(dep) && !registered.contains(dep)) {
        register(registry, all.get(dep), registered, all);
      }
    }

    JsLibraryFeatureFactory factory
        = new JsLibraryFeatureFactory(feature.gadgetJs, feature.containerJs);
    registered.add(feature.name);
    return registry.register(feature.name, feature.deps, factory);
  }

  /**
   * Parses the input into a dom tree.
   * @param xml
   * @param path The path the file was loaded from.
   * @param isResource True if the file was a resource.
   * @return A dom tree representing the feature.
   * @throws GadgetException
   */
  private ParsedFeature parse(String xml, String path, boolean isResource)
      throws GadgetException {

    Document doc;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      InputSource is = new InputSource(new StringReader(xml));
      doc = factory.newDocumentBuilder().parse(is);
    } catch (SAXException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e);
    } catch (ParserConfigurationException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e);
    }

    ParsedFeature feature = new ParsedFeature();

    feature.basePath = path;
    feature.isResource = isResource;

    NodeList nameNode = doc.getElementsByTagName("name");
    if (nameNode.getLength() != 1) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT);
    }
    feature.name = nameNode.item(0).getTextContent();

    NodeList gadgets = doc.getElementsByTagName("gadget");
    for (int i = 0, j = gadgets.getLength(); i < j; ++i) {
      processContext(feature, gadgets.item(i), false);
    }

    NodeList containers = doc.getElementsByTagName("container");
    for (int i = 0, j = containers.getLength(); i < j; ++i) {
      processContext(feature, containers.item(i), true);
    }

    NodeList dependencies = doc.getElementsByTagName("dependency");
    for (int i = 0, j = dependencies.getLength(); i < j; ++i) {
      feature.deps.add(dependencies.item(i).getTextContent());
    }

    return feature;
  }

  /**
   * Processes <gadget> and <container> tags and adds new libraries
   * to the feature.
   * @param feature
   * @param context
   * @param isContainer
   */
  private void processContext(ParsedFeature feature, Node context,
                              boolean isContainer) {
    NodeList libraries = context.getChildNodes();
    for (int i = 0, j = libraries.getLength(); i < j; ++i) {
      Node node = libraries.item(i);
      String nodeValue = node.getNodeName();
      if ("script".equals(nodeValue)) {
        NamedNodeMap attrs = node.getAttributes();
        Node srcNode = attrs.getNamedItem("src");
        String content;
        JsLibrary.Type type;
        if (srcNode == null) {
          type = JsLibrary.Type.INLINE;
          content = node.getTextContent();
        } else {
          content = srcNode.getTextContent();
          if (content.startsWith("http://")) {
            type = JsLibrary.Type.URL;
          } else if (content.startsWith("//")) {
            type = JsLibrary.Type.URL;
            content = content.substring(1);
          } else if (content.startsWith("res://")) {
            content = content.substring(6);
            type = JsLibrary.Type.RESOURCE;
          } else if (feature.isResource) {
            // Note: Any features loaded as resources will assume that their
            // paths point to resources as well.
            content = feature.basePath + content;
            type = JsLibrary.Type.RESOURCE;
          } else {
            content = feature.basePath + content;
            type = JsLibrary.Type.FILE;
          }
        }
        JsLibrary library = JsLibrary.create(type, content);
        if (library != null) {
          if (isContainer) {
            feature.containerJs.add(library);
          } else {
            feature.gadgetJs.add(library);
          }
        }
      }
    }
  }
}

/**
 * Temporary structure to represent the intermediary parse state.
 */
class ParsedFeature {
  public String name = "";
  public String basePath = "";
  public boolean isResource = false;
  public List<JsLibrary> containerJs = new LinkedList<JsLibrary>();
  public List<JsLibrary> gadgetJs = new LinkedList<JsLibrary>();
  public List<String> deps = new LinkedList<String>();
}