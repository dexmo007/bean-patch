package com.dexmohq.annotation.processing;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.*;

abstract class TypeMirrorLikeSet<E> implements Set<E> {

    private final Types types;
    private final List<E> list = new ArrayList<>();

    protected TypeMirrorLikeSet(Types types) {
        this.types = types;
    }

    protected TypeMirror[] getTypeMirrors(E e) {
        return ((TypeMirrorLike) e).getTypeMirrors();
    }

    private boolean areEqual(TypeMirror[] typeMirrors, TypeMirror[] otherTypeMirrors) {
        if (typeMirrors == null || otherTypeMirrors == null || typeMirrors.length != otherTypeMirrors.length) {
            return false;
        }
        for (int i = 0; i < typeMirrors.length; i++) {
            if (!types.isSameType(typeMirrors[i], otherTypeMirrors[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        final TypeMirror[] otherTypeMirrors = getTypeMirrors((E) o);
        for (final E e : list) {
            final TypeMirror[] typeMirrors = getTypeMirrors(e);
            if (areEqual(typeMirrors, otherTypeMirrors)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        if (contains(e)) {
            return false;
        }
        list.add(e);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        return list.removeIf(e -> areEqual(getTypeMirrors(e), getTypeMirrors((E) o)));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return c.stream().anyMatch(this::add);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return c.removeIf(e -> !c.contains(e));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return c.stream().anyMatch(this::remove);
    }

    @Override
    public void clear() {
        list.clear();
    }
}
