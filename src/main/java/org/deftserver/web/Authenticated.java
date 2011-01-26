package org.deftserver.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.deftserver.web.handler.RequestHandler;

/**
 * Annotation used by implementation of {@link RequestHandler} to show that a method requires that the request 
 * (current user) is authenticated (i.e your overridden method {@link RequestHandler#getCurrentUser} 
 * returns a non null value) 
 *
 */

@Retention(RetentionPolicy.RUNTIME)	
@Target(ElementType.METHOD)
public @interface Authenticated {}
