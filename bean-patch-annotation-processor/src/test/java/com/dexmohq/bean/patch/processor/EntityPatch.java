package com.dexmohq.bean.patch.processor;

import com.dexmohq.bean.patch.spi.Patch;

import java.util.List;

public class EntityPatch implements Patch<Entity> {

    private String text;


    private Integer foo;

    private List<Integer> numbers;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getFoo() {
        return foo;
    }


    public void setFoo(Integer foo) {
        this.foo = foo;
    }

    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers) {
        this.numbers = numbers;
    }
}
