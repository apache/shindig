package org.apache.shindig.common;

/**
 * A special class of Token representing the anonymous viewer/owner
 *
 * All methods except for isAnonymous will throw IllegalArgumentExceptions
 */
public class AnonymousSecurityToken implements SecurityToken {
  private static final SecurityToken instance = new AnonymousSecurityToken();

  /**
   * Private method, please use getInstance()
   */
  private AnonymousSecurityToken() {
  }

  public static SecurityToken getInstance() {
    return instance;
  }

  public String toSerialForm() {
    throw new IllegalArgumentException();
  }

  public String getOwnerId() {
    throw new IllegalArgumentException();
  }

  public String getViewerId() {
    throw new IllegalArgumentException();
  }

  public String getAppId() {
    throw new IllegalArgumentException();
  }

  public String getDomain() {
    throw new IllegalArgumentException();
  }

  public String getAppUrl() {
    throw new IllegalArgumentException();
  }

  public long getModuleId() {
    throw new IllegalArgumentException();
  }

  public String getUpdatedToken() {
    throw new IllegalArgumentException();
  }

  public String getTrustedJson() {
    throw new IllegalArgumentException();
  }

  public boolean isAnonymous() {
    return true;
  }
}
