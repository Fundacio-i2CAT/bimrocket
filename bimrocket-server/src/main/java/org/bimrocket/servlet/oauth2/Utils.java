package org.bimrocket.servlet.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Utils
{
    public static JsonNode decodeJWTToken(String token) throws JsonProcessingException
    {
        String[] parts = token.split("\\.");

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(payloadJson);
    }
}
