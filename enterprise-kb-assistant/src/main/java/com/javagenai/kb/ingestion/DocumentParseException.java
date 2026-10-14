package com.javagenai.kb.ingestion;

/** Raised when a parser cannot turn a document into structure. */
public class DocumentParseException extends RuntimeException {

    public DocumentParseException(String message) {
        super(message);
    }

    public DocumentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
