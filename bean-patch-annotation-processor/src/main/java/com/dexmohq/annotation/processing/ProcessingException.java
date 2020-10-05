package com.dexmohq.annotation.processing;

import javax.lang.model.element.Element;

public class ProcessingException extends RuntimeException {

    private final Element origin;

    public ProcessingException(Element origin, String message) {
        super(message);
        this.origin = origin;
    }
    public ProcessingException(Element origin, String format, Object... args) {
        this(origin, String.format(format, args));
    }

    public Element getOrigin() {
        return origin;
    }
}
