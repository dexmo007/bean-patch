package com.dexmohq.bean.patch.processor;

import com.dexmohq.bean.patch.processor.beans.PropertyDescriptor;

import java.util.HashMap;
import java.util.Map;

public class VariableNameGenerator {

    private final Map<String, Integer> names = new HashMap<>();

    public VariableNameGenerator(String... initialNames) {
        if (initialNames != null) {
            for (final String initialName : initialNames) {
                putVariable(initialName);
            }
        }
    }

    public Integer putVariable(String name) {
        final Integer count = names.get(name);
        if (count == null) {
            names.put(name, 1);
            return 0;
        }
        names.put(name, count + 1);
        return count;
    }

    public String nextName(String propertyName) {
        final String baseName = decapitalize(propertyName);
        final Integer count = putVariable(baseName);
        if (count > 0) {
            return baseName + count;
        }
        return baseName;
    }

    public String nextName(PropertyDescriptor property) {
        return nextName(property.getName());
    }

    private static String decapitalize(String cap) {
        return cap.substring(0, 1).toLowerCase()
                + cap.substring(1);
    }
}
