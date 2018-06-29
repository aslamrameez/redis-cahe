package com.rogers.validator.Annotations;

import com.rogers.validator.ValidatorAnnotationGetAction;
import play.mvc.With;
import scala.xml.dtd.DEFAULT;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@With(ValidatorAnnotationGetAction.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface QueryParamsValidator {

    Class value() default DEFAULT.class;
}
