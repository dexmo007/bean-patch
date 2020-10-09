package com.dexmohq.bean.patch.processor.gen;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.springframework.stereotype.Component;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public class SpringComponentModel implements ComponentModel {
    @Override
    public void postProcessType(TypeSpec.Builder type, ProcessingEnvironment env) {
        type.addAnnotation(Component.class);
    }
}
