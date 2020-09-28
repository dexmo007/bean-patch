package com.dexmohq.bean.patch.example;

import com.dexmohq.bean.patch.spi.EnablePatch;
import com.dexmohq.bean.patch.spi.Patch;

import java.util.List;

@EnablePatch
public class EntityPatch implements Patch<Entity> {

    private String text;

    private int foo;

    private List<Integer> numbers;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
