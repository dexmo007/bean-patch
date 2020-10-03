package com.dexmohq.bean.patch.example;

import com.dexmohq.bean.patch.Patchers;

public class Test {


    public static void main(String[] args) {
        final EntityPatcher patcher = Patchers.getPatcher(EntityPatcher.class);
        System.out.println(patcher);
    }
}
