package com.dexmohq.bean.patch.example;

import com.dexmohq.bean.patch.spi.Patch;
import com.dexmohq.bean.patch.spi.PatchProperty;
import com.dexmohq.bean.patch.spi.PatchType;

import java.util.List;

public class EntityPatch implements Patch<Entity> {

    private String text;
    @PatchProperty("foo")
    private Integer fooUpdateValue;
    @PatchProperty(value = "numbers", type = PatchType.ADD)
    private List<Integer> addNumbers;
    @PatchProperty(value = "numbers", type = PatchType.REMOVE)
    private List<Integer> removeNumbers;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getFooUpdateValue() {
        return fooUpdateValue;
    }

    public void setFooUpdateValue(Integer fooUpdateValue) {
        this.fooUpdateValue = fooUpdateValue;
    }

    public List<Integer> getAddNumbers() {
        return addNumbers;
    }

    public void setAddNumbers(List<Integer> addNumbers) {
        this.addNumbers = addNumbers;
    }

    public List<Integer> getRemoveNumbers() {
        return removeNumbers;
    }

    public void setRemoveNumbers(List<Integer> removeNumbers) {
        this.removeNumbers = removeNumbers;
    }
}
