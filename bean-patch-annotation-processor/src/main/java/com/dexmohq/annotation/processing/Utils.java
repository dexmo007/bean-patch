package com.dexmohq.annotation.processing;

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
import static javax.lang.model.element.ElementKind.*;
import static javax.lang.model.element.Modifier.*;

public class Utils {

    private final Types types;
    private final Elements elements;
//    private final Map<Class<?>, DeclaredType> classToTypeCache

    public Utils(Types types, Elements elements) {
        this.types = types;
        this.elements = elements;
    }

    public boolean hasPublicNoArgsConstructor(TypeElement typeElement) {
        for (final Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == CONSTRUCTOR
                    && element.getModifiers().contains(PUBLIC)
                    && ((ExecutableElement) element).getParameters().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean implementsInterface(TypeMirror type, Class<?> interfaceType) {
        return implementsInterface(type, elements.getTypeElement(interfaceType.getCanonicalName()));
    }

    public boolean implementsInterface(TypeMirror type, TypeElement interfaceType) {
        return implementsInterface(type, interfaceType.asType());
    }

    public boolean implementsInterface(TypeMirror type, TypeMirror interfaceType) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        final TypeMirror erasedInterfaceType = types.erasure(interfaceType);
        final TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getInterfaces().stream()
                .anyMatch(t -> types.isAssignable(types.erasure(t), erasedInterfaceType));
    }

    public boolean isCollectionType(TypeMirror type) {
        return implementsInterface(type, Collection.class);
    }

    public boolean isCollectionOfSupertypeOf(TypeMirror collectionType, TypeMirror elementType) {
        if (!isCollectionType(collectionType)) {
            return false;
        }
        final TypeMirror actualElementType = findElementTypeOfIterable(collectionType);
        return types.isSubtype(actualElementType, elementType);
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


    public TypeMirror findElementTypeOfIterable(TypeMirror collection) {
        if (collection.getKind() != TypeKind.DECLARED) {
            return null;
        }
        final TypeElement collectionTypeElement = elements.getTypeElement(Iterable.class.getCanonicalName());
        if (types.isSameType(((DeclaredType) collection).asElement().asType(), collectionTypeElement.asType())) {
            return ((DeclaredType) collection).getTypeArguments().get(0);
        }
        for (final TypeMirror typeMirror : types.directSupertypes(collection)) {
            final TypeMirror elementType = findElementTypeOfIterable(typeMirror);
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
