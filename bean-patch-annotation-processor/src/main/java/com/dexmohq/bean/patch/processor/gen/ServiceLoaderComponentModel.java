package com.dexmohq.bean.patch.processor.gen;

import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;

public class ServiceLoaderComponentModel implements ComponentModel {
    @Override
    public void postProcessFile(TypeElement origin, JavaFile impl, ProcessingEnvironment env) throws IOException {
        final String fqImpl = impl.packageName.isEmpty()
                ? impl.typeSpec.name
                : impl.packageName + "." + impl.typeSpec.name;
        final String resourceFile = "META-INF/services/" + origin.getQualifiedName().toString();
        final FileObject file = env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile, origin);
        final BufferedWriter writer = new BufferedWriter(file.openWriter());
        writer.write(fqImpl);
        writer.newLine();
        writer.flush();
        writer.close();
    }
}
