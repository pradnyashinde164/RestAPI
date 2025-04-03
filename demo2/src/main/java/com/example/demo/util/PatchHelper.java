package com.example.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public class PatchHelper {

    // ObjectMapper instance for converting objects to JSON and back
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Public method to apply a map of updates to an original object
    public static <T> T applyPatch(T original, Map<String, Object> updates) throws Exception {
        // Convert the original object and the updates map to JSON nodes
        JsonNode originalNode = objectMapper.convertValue(original, JsonNode.class);
        JsonNode updatesNode = objectMapper.convertValue(updates, JsonNode.class);

        // Merge the updates into the original node
        JsonNode mergedNode = merge(originalNode, updatesNode);

        // Convert the merged JSON node back to an instance of the original object's class
        return objectMapper.treeToValue(mergedNode, (Class<T>) original.getClass());
    }

    // Private method to recursively merge updates into the original JSON node
    private static JsonNode merge(JsonNode original, JsonNode updates) {
        // Iterate through each field in the updates
        Iterator<Map.Entry<String, JsonNode>> fields = updates.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> update = fields.next();
            String fieldName = update.getKey();
            JsonNode originalValue = original.get(fieldName);
            JsonNode updateValue = update.getValue();

            // If the original field is an object, recursively merge it
            if (originalValue != null && originalValue.isObject()) {
                merge(originalValue, updateValue);
            } else if (originalValue != null && originalValue.isArray() && updateValue.isArray()) {
                // If the field is an array, merge the arrays
                mergeArrays((ArrayNode) originalValue, (ArrayNode) updateValue);
            } else {
                // For primitive types or when the original field is not an object or array,
                // replace the original value with the update value
                if (original instanceof ObjectNode) {
                    ((ObjectNode) original).replace(fieldName, updateValue);
                }
            }
        }
        return original;
    }

    // Private method to merge two JSON arrays
    private static void mergeArrays(ArrayNode original, ArrayNode updates) {
        // Iterate over each element in the update array
        for (JsonNode update : updates) {
            boolean exists = false;
            String updateId = hasIdentifier(update);

            // Check if the update element exists in the original array by an identifier
            for (JsonNode orig : original) {
                if (updateId.equals(hasIdentifier(orig))) {
                    // If it exists, recursively merge the update into the original element
                    merge(orig, update);
                    exists = true;
                    break;
                }
            }
            // If it doesn't exist, add the new element to the original array
            if (!exists) {
                original.add(update);
            }
        }
    }

    // Helper method to get an identifier from a JSON node, used for matching elements in arrays
    private static String hasIdentifier(JsonNode node) {
        JsonNode objectIdNode = node.get("objectId");
        return objectIdNode != null ? objectIdNode.asText() : "";
    }
}
