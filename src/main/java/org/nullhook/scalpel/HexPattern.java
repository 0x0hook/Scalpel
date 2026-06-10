package org.nullhook.scalpel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class HexPattern {
    private final int[] values;
    private final boolean[] wildcards;

    private HexPattern(int[] values, boolean[] wildcards) {
        this.values = values;
        this.wildcards = wildcards;
    }

    static HexPattern parse(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return new HexPattern(new int[0], new boolean[0]);
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            byte[] bytes = trimmed.substring(1, trimmed.length() - 1).getBytes(StandardCharsets.UTF_8);
            int[] values = new int[bytes.length];
            boolean[] wildcards = new boolean[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                values[i] = bytes[i] & 0xff;
            }
            return new HexPattern(values, wildcards);
        }

        String clean = trimmed.replace("0x", "").replace("0X", "")
            .replace(",", " ").replace(":", " ").replace("-", " ");
        String[] parts = clean.contains(" ") ? clean.split("\\s+") : splitPairs(clean);
        List<Integer> values = new ArrayList<>();
        List<Boolean> wildcards = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (part.equals("?") || part.equals("??")) {
                values.add(0);
                wildcards.add(true);
                continue;
            }
            if (part.length() != 2 || !part.matches("[0-9a-fA-F]{2}")) {
                return new HexPattern(new int[0], new boolean[0]);
            }
            values.add(Integer.parseInt(part, 16));
            wildcards.add(false);
        }
        int[] v = new int[values.size()];
        boolean[] w = new boolean[wildcards.size()];
        for (int i = 0; i < values.size(); i++) {
            v[i] = values.get(i);
            w[i] = wildcards.get(i);
        }
        return new HexPattern(v, w);
    }

    int length() {
        return values.length;
    }

    boolean isEmpty() {
        return values.length == 0;
    }

    boolean matches(int offset, int value) {
        return wildcards[offset] || values[offset] == (value & 0xff);
    }

    private static String[] splitPairs(String clean) {
        if ((clean.length() & 1) != 0) {
            return new String[] {clean};
        }
        String[] parts = new String[clean.length() / 2];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = clean.substring(i * 2, i * 2 + 2);
        }
        return parts;
    }
}
