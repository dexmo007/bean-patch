package com.dexmohq.bean.patch.example;

import com.dexmohq.bean.patch.spi.Patcher;

@Patcher
public interface EntityPatcher {

    Entity applyPatch(Entity entity, Iterable<EntityPatch> patch);

    Entity applyPatch(Entity entity, EntityPatch patch);


    Entity applyPatch(Entity entity, EntityPatch patch, EntityPatch... morePatches);

}