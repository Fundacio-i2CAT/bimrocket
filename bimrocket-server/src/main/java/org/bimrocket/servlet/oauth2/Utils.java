package org.bimrocket.servlet.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 *
 * @author jordi.hernandez@i2cat.net
 */
public class Utils
{
    public static final String PROJECTISTA = "PROJECTISTA";
    public static final String VECTOR_UT_OGE = "VECTOR-UT-OGE";

    public static JsonNode decodeJWTToken(String token) throws JsonProcessingException
    {
        String[] parts = token.split("\\.");

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(payloadJson);
    }

    public static String escapeJsString(String value)
    {
      if (value == null) return "\"\"";
      return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static String generatePassword()
    {
      SecureRandom RANDOM = new SecureRandom();

      /**
       * Generate paswrod with format: prefix + zero-padded digits + specialChar
       * Example: Prova000#
      */
      String prefix = "Bimrocket";
      int digits = 4;
      String specials = "#";

      int max = (int) Math.pow(10, digits);
      int number = RANDOM.nextInt(max);

      String format = "%0" + digits + "d";
      String numStr = String.format(Locale.ROOT, format, number);

      return prefix + numStr + specials;
    }
}
