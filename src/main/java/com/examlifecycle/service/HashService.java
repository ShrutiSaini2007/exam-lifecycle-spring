package com.examlifecycle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Any object passed in must have a fixed, deterministic field order (a
 * record or a simple POJO) — Jackson serializes those in declaration
 * order, which is enough to make the hash reproducible without needing a
 * full canonical-JSON library for this prototype's simple payload shapes.
 */
@Service
public class HashService {

    private final ObjectMapper mapper;

    public HashService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String sha256Json(Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e); // never happens on the JVM
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash payload", e);
        }
    }
}
