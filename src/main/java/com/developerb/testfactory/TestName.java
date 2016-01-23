package com.developerb.testfactory;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The JVM won't allow us to use human readable strings as method names
 * so instead we add this annotation to each generated test method to
 * store a human readable version of what the test does.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface TestName {

    String value();

}
