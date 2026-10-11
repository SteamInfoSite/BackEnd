package com.steaminfosite.dto.api.price;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;

public class SteamPriceDataDeserializer
        extends JsonDeserializer<SteamPriceData> {

    @Override
    public SteamPriceData deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {
        //정상 객체는 DTO로 변환
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            return context.readValue(
                    parser,
                    SteamPriceData.class
            );
        }
        //빈 배열만 가격 정보가 없는 응답으로 허용
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            if (parser.nextToken() == JsonToken.END_ARRAY) {
                return new SteamPriceData();
            }

            throw JsonMappingException.from(
                    parser,
                    "스팀 가격 data에 빈 배열이 아닌 배열이 반환됐습니다"
            );
        }

        throw JsonMappingException.from(
                parser,
                "스팀 가격 data의 응답 형식이 올바르지 않습니다"
        );
    }
}