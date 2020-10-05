package com.dexmohq.bean.patch.processor;

import com.dexmohq.bean.patch.spi.Patcher;

import java.util.List;

@Patcher
public interface EntityPatcher {

    Entity applyPatch(Entity entity, Iterable<EntityPatch> patch);

}