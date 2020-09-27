package com.dexmohq.bean.patch.example;

import com.dexmohq.bean.patch.spi.EnablePatch;
import com.dexmohq.bean.patch.spi.Patch;

@EnablePatch
public class EntityPatch implements Patch<Entity> {

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
