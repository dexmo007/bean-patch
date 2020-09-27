package com.dexmohq.bean.patch.processor;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class TestUtils {

    public static File findSourceFile(Class<?> cls) {
        final URI uri;
        try {
            uri = cls.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("should not happen", e);
        }
        final String srcRoot;
        if (uri.getPath().endsWith("/test-classes/")) {
            srcRoot = "test";
        } else {
            srcRoot = "main";
        }
        final URI sourceFileUri = uri.resolve("../../src/" + srcRoot + "/java/" + cls.getCanonicalName().replace(".", "/") + ".java");
        final File sourceFile = new File(sourceFileUri);
        if (!sourceFile.isFile()) {
            throw new IllegalArgumentException("Source file not found in project for class: " + cls);
        }
        return sourceFile;
    }

}
