package com.dexmohq.bean.patch.processor;

import org.junit.jupiter.api.Test;

import javax.tools.*;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTests {


    @Test
    void testBasic() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
        final Iterable<? extends JavaFileObject> javaFiles = fileManager.getJavaFileObjects(TestUtils.findSourceFile(TestCase.class));

        final JavaCompiler.CompilationTask task = compiler.getTask(new StringWriter(),
                fileManager,
                diagnosticCollector,
                List.of("-proc:only"),
                null,
                javaFiles);
        task.setProcessors(List.of(new EnablePatchProcessor()));
        task.call();

        assertThat(diagnosticCollector.getDiagnostics()).hasSize(1)
                .allSatisfy(diagnostic -> {
                    assertThat(diagnostic.getKind()).isEqualTo(Diagnostic.Kind.ERROR);
                    assertThat(diagnostic.getMessage(null)).isEqualTo("Class annotated with @EnablePatch must implement Patch<T>");
                    assertThat(diagnostic.getLineNumber()).isEqualTo(6);
                });
    }

    @Test
    void testPatcher() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
        final Iterable<? extends JavaFileObject> javaFiles = fileManager.getJavaFileObjects(TestUtils.findSourceFile(EntityPatcher.class));

        final JavaCompiler.CompilationTask task = compiler.getTask(new StringWriter(),
                fileManager,
                diagnosticCollector,
                List.of("-proc:only"),
                null,
                javaFiles);
        task.setProcessors(List.of(new EnablePatchProcessor()));
        task.call();

        assertThat(diagnosticCollector.getDiagnostics()).hasSize(0);
    }

}
