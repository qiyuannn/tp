package trackflow.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.commons.exceptions.DataLoadingException;
import trackflow.testutil.SerializableTestClass;

/**
 * Tests JSON Read and Write
 */
public class JsonUtilTest {

    @TempDir
    public Path tempDir;

    // EP: valid serializable object to JSON file
    @Test
    public void serializeObjectToJsonFile_noExceptionThrown() throws IOException {
        Path serializationFile = tempDir.resolve("serialize.json");
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        serializableTestClass.setTestValues();

        JsonUtil.serializeObjectToJsonFile(serializationFile, serializableTestClass);

        assertEquals(FileUtil.readFromFile(serializationFile), SerializableTestClass.JSON_STRING_REPRESENTATION);
    }

    // EP: valid JSON file to object deserialization
    @Test
    public void deserializeObjectFromJsonFile_noExceptionThrown() throws IOException {
        Path serializationFile = tempDir.resolve("serialize.json");
        FileUtil.writeToFile(serializationFile, SerializableTestClass.JSON_STRING_REPRESENTATION);

        SerializableTestClass serializableTestClass = JsonUtil
                .deserializeObjectFromJsonFile(serializationFile, SerializableTestClass.class);

        assertEquals(serializableTestClass.getName(), SerializableTestClass.getNameTestValue());
        assertEquals(serializableTestClass.getListOfLocalDateTimes(), SerializableTestClass.getListTestValues());
        assertEquals(serializableTestClass.getMapOfIntegerToString(), SerializableTestClass.getHashMapTestValues());
    }

    // EP: round-trip serialization and deserialization of valid object
    @Test
    public void jsonUtil_writeThenReadObjectToJson_correctObject() throws IOException {
        SerializableTestClass original = new SerializableTestClass();
        original.setTestValues();

        String jsonString = JsonUtil.toJsonString(original);
        assertEquals(SerializableTestClass.JSON_STRING_REPRESENTATION, jsonString);

        SerializableTestClass deserialized = JsonUtil.fromJsonString(jsonString, SerializableTestClass.class);
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getListOfLocalDateTimes(), deserialized.getListOfLocalDateTimes());
        assertEquals(original.getMapOfIntegerToString(), deserialized.getMapOfIntegerToString());
    }

    @Test
    public void readJsonFile_missingOrInvalid_handlesGracefully() throws Exception {
        // EP: non-existent file path (returns empty Optional)
        Path missingFile = tempDir.resolve("missing.json");
        assertTrue(JsonUtil.readJsonFile(missingFile, SerializableTestClass.class).isEmpty());

        // EP: file containing JSON null root (rejected with DataLoadingException)
        Path nullRootFile = tempDir.resolve("nullRoot.json");
        Files.writeString(nullRootFile, "null"); // Boundary value
        assertThrows(DataLoadingException.class, () ->
                JsonUtil.readJsonFile(nullRootFile, SerializableTestClass.class));

        // EP: malformed JSON syntax (rejected with DataLoadingException)
        Path malformedFile = tempDir.resolve("malformed.json");
        Files.writeString(malformedFile, "{invalid json");
        assertThrows(DataLoadingException.class, () ->
                JsonUtil.readJsonFile(malformedFile, SerializableTestClass.class));
    }

    @Test
    public void fromJsonString_singleRootWithWhitespace_preservesValueAndNullContract() throws IOException {
        // EP: valid JSON object with surrounding whitespace
        assertEquals(Map.of("message", "literal } { text"),
                JsonUtil.fromJsonString(" {\"message\":\"literal } { text\"} \t\r\n", Map.class));

        // EP: valid JSON primitive (number) with trailing whitespace
        assertEquals(42, JsonUtil.fromJsonString("42 \n", Integer.class));

        // EP: JSON null literal with trailing whitespace (returns null)
        assertNull(JsonUtil.fromJsonString("null \t", Object.class)); // Boundary value
    }

    // EP: invalid JSON string (extra root values, trailing garbage, or truncated syntax)
    @Test
    public void fromJsonString_extraRootsGarbageOrTruncation_throwsIoException() {
        for (String json : List.of("{} GARBAGE", "{} {}", "[] []", "42 43", "null {}", "{\"a\":1")) {
            assertThrows(IOException.class, () -> JsonUtil.fromJsonString(json, Object.class));
        }
    }
}
