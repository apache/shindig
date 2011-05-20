package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.EmbeddedExperienceImpl;

import com.google.inject.ImplementedBy;

/**
 * Represents an embedded experience that may be inlined.
 * 
 */
@ImplementedBy(EmbeddedExperienceImpl.class)
@Exportablebean
public interface EmbeddedExperience {

  /**
   * Fields that represent JSON elements for an embedded experience.
   */
  public static enum Field {
    GADGET("gadget"), CONTEXT("context"), PREVIEWIMAGE("previewImage"), URL("url");

    /*
     * The name of the JSON element.
     */
    private final String jsonString;

    /**
     * Constructs the field base for the JSON element.
     * 
     * @param jsonString
     *          the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * Returns the name of the JSON element.
     * 
     * @return String the name of the JSON element
     */
    public String toString() {
      return jsonString;
    }
  }

  /**
   * Gets the URL to the gadget definition for this embedded experience
   * 
   * @return the URL to the gadget definintion for this embedded experience
   */
  String getGadget();

  /**
   * Sets the URL to the gadget definition for this embedded experience
   * 
   * @param gadget
   *          the URL to the gadget definition for this embedded experience
   */
  void setGadget(String gadget);

  /**
   * Gets the contextual data for this embedded experience
   * 
   * @return the contextual data for this embedded experience
   */
  Object getContext();

  /**
   * Sets the contextual data for the embedded experience
   * 
   * @param context
   *          the contextual data for this embedded experience
   */
  void setContext(Object context);

  /**
   * Gets the URL to an image that may act as a preview for this embedded
   * experience
   * 
   * @return the URL to an image that may act as a preview for this embedded
   *         experiece
   */
  String getPreviewImage();

  /**
   * Set the URL to an image that may act as a preview for this embedded
   * experience
   * 
   * @param previewImage
   *          the URL to an image that may act as a preview for this embedded
   *          experience
   */
  void setPreviewImage(String previewImage);

  /**
   * Gets the URL that may be used as an embedded experience
   * 
   * @return the URL that may be used as an embedded experience
   */
  String getUrl();

  /**
   * Sets the URL that may be used as an embedded experience
   * 
   * @param url
   *          the URL that may be used as an embedded experience
   */
  void setUrl(String url);
}
