package org.apache.shindig.gadgets.rewrite;

/**
 * Exceptions thrown during content rewriting.
 *
 * These exceptions will usually translate directly into an end-user error message, so they should
 * be easily localizable.
 */
public class RewritingException extends Exception {
  public RewritingException(Throwable t) {
    super(t);
  }

  public RewritingException(String message) {
    super(message);
  }

  public RewritingException(String message, Throwable t) {
    super(message, t);
  }

}
