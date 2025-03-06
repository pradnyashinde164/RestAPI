package com.example.demo.controller;

import com.example.demo.model.Plan;
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
            //Validate JSON against schema
            JSONObject planJson = new JSONObject(planJsonString);
            planSchema.validate(planJson);

            //Map JSON to Plan object
            ObjectMapper objectMapper = new ObjectMapper();
            Plan plan = objectMapper.readValue(planJsonString, Plan.class);

            //Store plan in Redis
            ValueOperations<String, Plan> ops = redisTemplate.opsForValue();
            ops.set(plan.getObjectId(), plan);

            //Generate ETag for the plan
            String eTag = generateETag(plan);

            logger.info("Plan stored successfully for objectId: " + plan.getObjectId());
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
            logger.warning("Plan not found for objectId: " + objectId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Plan not found");
        }

        String currentETag = generateETag(plan);
        if (currentETag.equals(eTagHeader)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        logger.info("Plan retrieved successfully for objectId: " + objectId);
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
