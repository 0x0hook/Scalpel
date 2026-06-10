package org.nullhook.scalpel;

final class HexPatchResult {
    final boolean ok;
    final String message;

    private HexPatchResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    static HexPatchResult ok(String message) {
        return new HexPatchResult(true, message);
    }

    static HexPatchResult fail(String message) {
        return new HexPatchResult(false, message);
    }
}
