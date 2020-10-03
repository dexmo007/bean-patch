package com.dexmohq.bean.patch.processor;

import javax.lang.model.element.Element;

public class ProcessingException extends RuntimeException {

    private final Element origin;

    public ProcessingException(Element origin, String message) {
        super(message);
        this.origin = origin;
    }

    public Element getOrigin() {
        return origin;
    }
}
