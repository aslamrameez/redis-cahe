package com.rogers.validator.Annotations;

import com.rogers.validator.ValidatorAnnotationPostAction;
import play.mvc.With;

import javax.validation.groups.Default;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@With(ValidatorAnnotationPostAction.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PostBodyValidator {

    Class value() default Default.class;
}
