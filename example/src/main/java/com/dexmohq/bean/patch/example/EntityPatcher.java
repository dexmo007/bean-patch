package com.dexmohq.bean.patch.example;

import com.dexmohq.bean.patch.spi.Patcher;

import java.util.List;

@Patcher
public interface EntityPatcher {


    Entity applyPatch(Entity entity, List<EntityPatch> patch);

}