package com.dexmohq.bean.patch.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = TestSpring.class)
public class TestSpring {

    public static void main(String[] args) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestSpring.class);
        final EntityPatcher entityPatcher = context.getBean(EntityPatcher.class);
        System.out.println(entityPatcher);
    }
}
