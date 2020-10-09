package com.dexmohq.bean.patch.processor;

import com.dexmohq.annotation.processing.ProcessingException;
import com.dexmohq.annotation.processing.Utils;
import com.dexmohq.bean.patch.processor.beans.PropertiesIntrospector;
import com.dexmohq.bean.patch.processor.beans.PropertyDescriptor;
import com.dexmohq.bean.patch.processor.model.*;
import com.dexmohq.bean.patch.spi.PatchIgnore;
import com.dexmohq.bean.patch.spi.PatchProperty;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class PatchIntrospector {

    private final PropertiesIntrospector introspector;
    private final Types types;
    private final Utils utils;

    public PatchIntrospector(PropertiesIntrospector introspector, Types types, Utils utils) {
        this.introspector = introspector;
        this.types = types;
        this.utils = utils;
    }

    public PatchDefinition createPatchDefinition(PatchTypePair pair) {
        final PatchDefinition patchDefinition = new PatchDefinition(pair);
        final var entityProperties = introspector.getProperties(pair.getEntityTypeElement());
        final var patchProperties = introspector.getProperties(pair.getPatchTypeElement());
        for (final PropertyDescriptor patchProperty : patchProperties.values()) {
            if (patchProperty.isAnnotationPresent(PatchIgnore.class)) {
                continue;
            }
            final PatchPropertyInfo patchPropertyInfo = PatchPropertyInfo.from(patchProperty, patchProperty.getAnnotation(PatchProperty.class).orElse(null));
            final PropertyDescriptor targetProperty = entityProperties.get(patchPropertyInfo.getTarget());
            final var patchPropertyOrigin = patchProperty.getGetter();
            check(targetProperty != null,
                    patchPropertyOrigin,
                    "Target property '%s' does not exist on entity of type %s",
                    patchPropertyInfo.getTarget(), pair.getEntityType()
            );
            final var entityPropertyType = targetProperty.getType();
            final var patchPropertyType = patchProperty.getType();
            final Cardinality sourceCardinality;
            switch (patchPropertyInfo.getType()) {
                case SET:
                    check(types.isAssignable(patchPropertyType, entityPropertyType),
                            patchPropertyOrigin,
                            "When using PatchType.SET the patch type must be a subtype of the entity type, but %s isn't a subtype of %s",
                            patchPropertyType, entityPropertyType
                    );
                    sourceCardinality = Cardinality.ONE;
                    break;
                case ADD:
                case REMOVE:
                    check(utils.isCollectionType(entityPropertyType),
                            patchPropertyOrigin,
                            "Entity property type must be a collection to perform ADD, but was: %s",
                            entityPropertyType);
                    if (utils.isCollectionOfSupertypeOf(entityPropertyType, patchPropertyType)) {
                        sourceCardinality = Cardinality.ONE;
                    } else {
                        check(utils.isCollectionType(patchPropertyType), patchPropertyOrigin,
                                "Patch type does not conform to entity type");
                        final TypeMirror patchPropertyElementType = utils.findElementTypeOfIterable(patchPropertyType);
                        check(utils.isCollectionOfSupertypeOf(entityPropertyType, patchPropertyElementType),
                                patchPropertyOrigin,
                                "Patch values don't fit in entity collection");
                        sourceCardinality = Cardinality.MANY;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + patchPropertyInfo.getType());
            }
            final PatchPropertyDefinition def = new PatchPropertyDefinition(patchProperty, targetProperty, patchPropertyInfo.getType(), sourceCardinality);
            patchDefinition.addPatch(def);
        }
        return patchDefinition;
    }

    private void check(boolean assertion, Element origin, String msg, Object... args) {
        if (!assertion) {
            throw new ProcessingException(origin, msg, args);
        }
    }
}
