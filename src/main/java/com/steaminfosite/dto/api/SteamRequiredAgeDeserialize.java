package com.steaminfosite.dto.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

//required_age값중 극히 일부 다른 타입을 Long으로 변환
public class SteamRequiredAgeDeserialize extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {
        String value = parser.getValueAsString();

        if (value == null || value.isBlank()) {
            return null;
        }
        String number = value.replaceAll("[^0-9]", "");

        return number.isBlank() ? null : Long.parseLong(number);
    }
}