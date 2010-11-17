package org.apache.shindig.gadgets.rewrite;

import com.google.inject.BindingAnnotation;

import org.apache.shindig.config.ContainerConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that specifies a list of rewriters with the rewriteFlow and
 * container they are meant to be applied to.
 */
@BindingAnnotation
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseRewriterList {

  // Enum of rewrite flows being used.
  public enum RewriteFlow {
    DEFAULT,
    REQUEST_PIPELINE,
    ACCELERATE,
    DUMMY_FLOW
  }

  // The flow id signifying what type of rewriting is done.
  RewriteFlow rewriteFlow();

  // The container context.
  String container() default ContainerConfig.DEFAULT_CONTAINER;
}
