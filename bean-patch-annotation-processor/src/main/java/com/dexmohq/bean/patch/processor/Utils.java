package com.dexmohq.bean.patch.processor;

import com.google.common.base.CaseFormat;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;

public class Utils {

    private final Types types;
    private final Elements elements;

    public Utils(Types types, Elements elements) {
        this.types = types;
        this.elements = elements;
    }

    public boolean implementsInterface(TypeMirror type, TypeElement interfaceType) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        final TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getInterfaces().stream()
                .anyMatch(t -> types.isAssignable(types.erasure(t), interfaceType.asType()));
    }

    public boolean implementsInterfaceConcretely(TypeMirror type, TypeElement interfaceType, TypeMirror... typeVariableAssignments) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        final TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        final int expectedNoOfArgs = typeVariableAssignments == null ? 0 : typeVariableAssignments.length;
        if (interfaceType.getTypeParameters().size() != expectedNoOfArgs) {
            throw new IllegalArgumentException(String.format("Interface of type %s expects %d type arguments, but it was checked for %d", interfaceType.getSimpleName(), interfaceType.getTypeParameters().size(), expectedNoOfArgs));
        }
        final Optional<DeclaredType> interfaceImpl = typeElement.getInterfaces().stream()
                .filter(t -> types.isAssignable(types.erasure(t), interfaceType.asType()))
                .map(t -> ((DeclaredType) t))
                .findFirst();
        if (interfaceImpl.isEmpty()) {
            return false;
        }
        if (typeVariableAssignments == null) {
            return true;
        }
        final List<? extends TypeMirror> typeArgs = interfaceImpl.get().getTypeArguments();
        return range(0, typeArgs.size())
                .allMatch(i -> types.isSameType(typeArgs.get(i), typeVariableAssignments[i]));
    }



    public <T extends TypeMirror> Comparator<T> typeMirrorComparator() {
        return (o1, o2) -> {
            if (types.isSameType(o1, o2)) {
                return 0;
            }
            return o1.toString().compareTo(o2.toString());
        };
    }

    public <E extends TypeMirror> Set<E> typeMirrorSet() {
        return new TypeMirrorSet<>(types);
    }

    public <E> Set<E> typeMirrorLikeSet() {
        return new TypeMirrorLikeSet<>(types) {
        };
    }

    public <E> Set<E> typeMirrorLikeSet(Function<? super E, ? extends TypeMirror[]> getTypeMirrors) {
        return new TypeMirrorLikeSet<>(types) {
            @Override
            protected TypeMirror[] getTypeMirrors(E e) {
                return getTypeMirrors.apply(e);
            }
        };
    }


    public <T extends TypeMirror> Collector<T, ?, Set<T>> toTypeMirrorSet() {
        return Collectors.toCollection(this::typeMirrorSet);
    }

    public <T> Collector<T, ?, Set<T>> toTypeMirrorLikeSet() {
        return Collectors.toCollection(this::typeMirrorLikeSet);
    }

    public <T> Collector<T, ?, Set<T>> toTypeMirrorLikeSet(Function<? super T, ? extends TypeMirror[]> getTypeMirrors) {
        return Collectors.toCollection(() -> typeMirrorLikeSet(getTypeMirrors));
    }

    public boolean areEqual(TypeMirrorLike o1, TypeMirrorLike o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        final TypeMirror[] typeMirrors = o1.getTypeMirrors();
        final TypeMirror[] otherTypeMirrors = o2.getTypeMirrors();
        if (typeMirrors.length != otherTypeMirrors.length) {
            return false;
        }
        for (int i = 0; i < typeMirrors.length; i++) {
            if (!types.isSameType(typeMirrors[i], otherTypeMirrors[i])) {
                return false;
            }
        }
        return true;
    }


    public TypeMirror findElementTypeOfCollection(TypeMirror collection) {
        if (collection.getKind() != TypeKind.DECLARED) {
            return null;
        }
        final TypeElement collectionTypeElement = elements.getTypeElement(Collection.class.getCanonicalName());
        if (types.isSameType(((DeclaredType) collection).asElement().asType(), collectionTypeElement.asType())) {
            return ((DeclaredType) collection).getTypeArguments().get(0);
        }
        for (final TypeMirror typeMirror : types.directSupertypes(collection)) {
            final TypeMirror elementType = findElementTypeOfCollection(typeMirror);
            if (elementType != null) {
                return elementType;
            }
        }
        return null;
    }

    public <K, V> Map<K, V> newTypeMirrorLikeMap(Function<? super K, ? extends TypeMirror[]> toTypeMirrors) {
        return new TypeMirrorLikeMap<>(this) {
            @Override
            protected TypeMirrorLike toTypeMirrorLike(K key) {
                return () -> toTypeMirrors.apply(key);
            }
        };
    }

    public <K extends TypeMirrorLike, V> Map<K, V> newTypeMirrorLikeMap() {
        return new TypeMirrorLikeMap<>(this) {
            @Override
            protected TypeMirrorLike toTypeMirrorLike(K key) {
                return key;
            }
        };
    }

    public <K extends TypeMirror, V> Map<K, V> newTypeMirrorMap() {
        return new TypeMirrorLikeMap<>(this) {
            @Override
            protected TypeMirrorLike toTypeMirrorLike(K key) {
                return () -> new TypeMirror[]{key};
            }
        };
    }
}
