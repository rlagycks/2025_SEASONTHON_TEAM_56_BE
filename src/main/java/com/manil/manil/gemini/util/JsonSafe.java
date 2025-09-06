package com.manil.manil.gemini.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manil.manil.gemini.dto.response.GenerateContentResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JsonSafe {
    private static final ObjectMapper om = new ObjectMapper();
    private JsonSafe() {}

    public static Map<String,Object> readMap(String s) {
        try { return om.readValue(s, Map.class); }
        catch (Exception e) { throw new RuntimeException("Invalid JSON: " + s, e); }
    }
    public static Optional<String> firstText(GenerateContentResponse res) {
        if (res == null || res.getCandidates() == null) return Optional.empty();
        for (var c : res.getCandidates()) {
            if (c.getContent() == null || c.getContent().getParts() == null) continue;
            for (var p : c.getContent().getParts()) {
                if (p.getText() != null && !p.getText().isBlank()) return Optional.of(p.getText());
            }
        }
        return Optional.empty();
    }
    public static String getString(Map<String,Object> map, String key) {
        var v = map.get(key); return v == null ? "" : String.valueOf(v);
    }
    @SuppressWarnings("unchecked")
    public static List<String> getStringList(Map<String,Object> map, String key) {
        var v = map.get(key);
        if (v == null) return List.of();
        if (v instanceof List<?> l) return l.stream().map(String::valueOf).toList();
        return List.of(String.valueOf(v));
    }
}