package org.deft.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by implementation of {@link RequestHandler} to denote a method as asynchronous (i.e don't close the 
 * Http connection until the client invokes the finish method)
 *
 */

@Retention(RetentionPolicy.RUNTIME)	
@Target(ElementType.METHOD)
public @interface Asynchronous {}
