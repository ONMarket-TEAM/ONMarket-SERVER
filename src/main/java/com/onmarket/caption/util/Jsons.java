package com.onmarket.caption.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsons {
    private static final ObjectMapper om = new ObjectMapper();
    public static String to(Object o) {
        try { return om.writeValueAsString(o); } catch (JsonProcessingException e) { return "{}"; }
    }
}