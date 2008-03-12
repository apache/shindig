package org.apache.shindig.social.samplecontainer;

import org.w3c.dom.Document;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentRequest;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;
import java.io.IOException;

/**
 * @author Cassandra Doll <doll@google.com>
 */
public class XmlStateFileFetcher {
  private String stateFile;
  private Document document;

  // TODO: Prob change to a Uri param, let the stateFile setter deal
  // with the exception.
  public XmlStateFileFetcher(String stateFile) {
    this.stateFile = stateFile;
  }

  public Document fetchStateDocument(boolean useCache) {
    if (useCache && document != null) {
      return document;
    }

    URI uri;
    try {
      uri = new URI(stateFile);
    } catch (URISyntaxException e) {
      throw new RuntimeException("The state file " + stateFile
          + " does not point to a valid uri", e);
    }

    // TODO: Eventually get the fetcher and processing options from a
    // config file, just like the GadgetServer
    RemoteContentFetcher fetcher = new BasicRemoteContentFetcher(1024 * 1024);
    RemoteContent xml = fetcher.fetch(new RemoteContentRequest(uri));

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
}
