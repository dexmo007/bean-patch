package com.dexmohq.bean.patch.processor.beans;

import com.dexmohq.bean.patch.processor.ProcessingException;

import javax.lang.model.element.Element;

public class IntrospectionException extends ProcessingException {
    public IntrospectionException(Element origin, String message) {
        super(origin, message);
    }
}
