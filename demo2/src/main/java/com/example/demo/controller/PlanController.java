package com.example.demo.controller;

import com.example.demo.model.Plan;
import com.example.demo.util.PatchHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.Map;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/plans")
@Validated
public class PlanController {

    private static final Logger logger = Logger.getLogger(PlanController.class.getName());
    private final RedisTemplate<String, Plan> redisTemplate;
    private final Schema planSchema;

    public PlanController(RedisTemplate<String, Plan> redisTemplate) {
        this.redisTemplate = redisTemplate;

        //Loading JSON schema from the resources directory
        try {
            String schemaContent = new String(Files.readAllBytes(
                    Paths.get(new ClassPathResource("schema.json").getURI())));
            JSONObject schemaJson = new JSONObject(schemaContent);
            this.planSchema = SchemaLoader.load(schemaJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON schema", e);
        }
    }

    //Post plan data
    @PostMapping
    public ResponseEntity<String> storePlan(@Valid @RequestBody String planJsonString) {
        try {
            JSONObject planJson = new JSONObject(planJsonString);
            planSchema.validate(planJson);

            ObjectMapper objectMapper = new ObjectMapper();
            Plan plan = objectMapper.readValue(planJsonString, Plan.class);

            ValueOperations<String, Plan> ops = redisTemplate.opsForValue();
            ops.set(plan.getObjectId(), plan);

            String eTag = generateETag(plan);
            return ResponseEntity.status(HttpStatus.CREATED).header("ETag", eTag).body("Plan stored successfully");
        } catch (Exception e) {
            logger.severe("Error storing plan: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid plan data: " + e.getMessage());
        }
    }


    //Get Plan data
    @GetMapping("/{objectId}")
    public ResponseEntity<?> getPlan(@PathVariable String objectId, @RequestHeader(value = "If-None-Match", required = false) String eTagHeader) {
        ValueOperations<String, Plan> ops = redisTemplate.opsForValue();
        Plan plan = ops.get(objectId);

        if (plan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Plan not found");
        }

        String currentETag = generateETag(plan);
        if (eTagHeader != null && eTagHeader.equals(currentETag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok().header("ETag", currentETag).body(plan);
    }


    //Delete Plan data
    @DeleteMapping("/{objectId}")
    public ResponseEntity<String> deletePlan(@PathVariable String objectId) {
        ValueOperations<String, Plan> ops = redisTemplate.opsForValue();
        Plan plan = ops.get(objectId);

        if (plan == null) {
            logger.warning("Plan not found for deletion: " + objectId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Plan not found");
        }

        redisTemplate.delete(objectId);
        logger.info("Plan deleted successfully for objectId: " + objectId);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{objectId}")
    public ResponseEntity<String> patchPlan(@PathVariable String objectId, @RequestBody Map<String, Object> updates, @RequestHeader(value = "If-Match", required = false) String clientETag) {
        // Retrieve the Plan object from Redis using the objectId
        ValueOperations<String, Plan> ops = redisTemplate.opsForValue();
        Plan existingPlan = ops.get(objectId);
        // Check if the Plan exists
        if (existingPlan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Plan not found");
        }

        // Only check ETag match if clientETag is provided
        String currentETag = generateETag(existingPlan);
        if (clientETag != null && !currentETag.equals(clientETag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("ETag mismatch");
        }

        try {
            // Apply the updates to the existing Plan object using the PatchHelper utility class
            Plan updatedPlan = PatchHelper.applyPatch(existingPlan, updates);
            ops.set(objectId, updatedPlan);// Save the updated Plan back to Redis
            String newETag = generateETag(updatedPlan);// Generate a new ETag for the updated Plan
            return ResponseEntity.ok().header("ETag", newETag).body("Plan updated successfully");
        } catch (Exception e) {
            logger.severe("Error processing PATCH request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing PATCH request: " + e.getMessage());
        }
    }




    @PutMapping("/{objectId}")
    public ResponseEntity<String> updatePlan(@PathVariable String objectId, @Valid @RequestBody Plan plan, @RequestHeader(value = "If-Match") String eTag) {
        ValueOperations<String, Plan> ops = redisTemplate.opsForValue();
        Plan currentPlan = ops.get(objectId);
        if (currentPlan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Plan not found");
        }

        String currentETag = generateETag(currentPlan);
        if (!eTag.equals(currentETag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("ETag mismatch");
        }

        ops.set(objectId, plan);
        return ResponseEntity.ok("Plan updated successfully");
    }

    //Generate ETag
    private String generateETag(Plan plan) {
        //return Integer.toHexString(plan.hashCode());
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(plan); // Convert Plan to JSON string

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash); // Convert hash to a Base64 string
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new RuntimeException("Error generating ETag", e);
        }
    }
}
