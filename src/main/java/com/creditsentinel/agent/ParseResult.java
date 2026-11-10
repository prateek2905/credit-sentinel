package com.creditsentinel.agent;

public record ParseResult(MemoDraft draft, String error) {

    public static ParseResult ok(MemoDraft draft) {
        return new ParseResult(draft, null);
    }

    public static ParseResult error(String error) {
        return new ParseResult(null, error);
    }

    public boolean ok() {
        return draft != null;
    }
}
