package com.dexmohq.bean.patch.processor.gen;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;

public interface ComponentModel {

    default void postProcessType(TypeSpec.Builder type, ProcessingEnvironment env)  {

    }

    default void postProcessFile(TypeElement origin, JavaFile file, ProcessingEnvironment env) throws IOException {

    }

}
