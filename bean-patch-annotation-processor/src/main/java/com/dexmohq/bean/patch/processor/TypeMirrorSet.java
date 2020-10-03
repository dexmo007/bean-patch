package com.dexmohq.bean.patch.processor;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

class TypeMirrorSet<T extends TypeMirror> extends TypeMirrorLikeSet<T> {

    TypeMirrorSet(Types types) {
        super(types);
    }

    @Override
    protected TypeMirror[] getTypeMirrors(T t) {
        return new TypeMirror[]{t};
    }

}
