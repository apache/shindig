package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.ExtensionImpl;

import com.google.inject.ImplementedBy;

/**
 * A generic class to represent extensions to data models.
 */
@ImplementedBy(ExtensionImpl.class)
@Exportablebean
public interface Extension {

  public static enum Field {
    EMBED("embed"); // Embedded Experiences

    /**
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
   * Gets the embedded experience for this activity.
   * 
   * @return the embedded experience for this activity
   */
  EmbeddedExperience getEmbed();

  /**
   * Sets the emnbedded experience for this activity.
   * 
   * @param embed
   *          the embedded experience to set
   */
  void setEmbed(EmbeddedExperience embed);
}
