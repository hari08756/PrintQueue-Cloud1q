package com.printqueue;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@org.springframework.context.annotation.Profile("!cloud")
@RestController
public class TestApi {
    @GetMapping("/api/test")
    public Map<String, String> test() {
        return Map.of(
            "app", "PrintQueue Phase 5",
            "status", "ok",
            "message", "Local test endpoint working",
            "timestamp", Instant.now().toString()
        );
    }
}
