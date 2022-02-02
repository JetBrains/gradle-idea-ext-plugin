package org.jetbrains.gradle.ext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import groovy.json.JsonOutput;

import java.io.IOException;
import java.io.StringWriter;

public class SerializationUtil {

    public static String prettyPrintJSON(Object object) {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
        StringWriter sw = new StringWriter();
        JsonWriter jsonWriter;
        try {
            jsonWriter = gson.newJsonWriter(sw);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create new JsonWriter using GSON", e);
        }
        jsonWriter.setIndent("    ");
        if (object instanceof JsonElement) {
            gson.toJson((JsonElement) object, jsonWriter);
        } else {
            gson.toJson(gson.toJsonTree(object), jsonWriter);
        }
        return sw.toString();
    }

    public static String prettyPrintJsonStr(String rawJson) {
        JsonElement jsonElement = JsonParser.parseString(rawJson);
        return prettyPrintJSON(jsonElement);
    }
}

