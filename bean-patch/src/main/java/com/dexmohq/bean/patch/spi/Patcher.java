package com.dexmohq.bean.patch.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Patcher {

    ComponentModel componentModel() default ComponentModel.SERVICE_LOADER;

    enum ComponentModel {
        SERVICE_LOADER,SPRING;
    }
}
