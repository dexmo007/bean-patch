package com.dexmohq.bean.patch.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface PatchProperty {

    String value() default "";

    PatchType type() default PatchType.SET;

    Class<? extends PropertyPatcher> patcher() default PropertyPatcher.class;

    Class<? extends Resolver> resolver() default Resolver.class;

}
