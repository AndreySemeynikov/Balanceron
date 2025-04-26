package com.andrey.semeynikov.balanceron.controller;

import com.andrey.semeynikov.balanceron.service.AlgorithmManager;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loadbalancer")
@RequiredArgsConstructor
public class LoadBalancerApiController {

    private final AlgorithmManager algorithmManager;
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/algorithm")
    public Map<String, Object> getCurrentAlgorithm() {
        Map<String, Object> response = new HashMap<>();
        response.put("currentAlgorithm", algorithmManager.getCurrentAlgorithmName());
        response.put("description", algorithmManager.getCurrentAlgorithmDescription());
        response.put("availableAlgorithms", algorithmManager.getAvailableAlgorithms());

        // Добавляем информацию о том, загружен ли алгоритм из Redis
        String redisAlgorithm = redisTemplate.opsForValue().get("balanceron:current_algorithm");
        response.put("loadedFromRedis", redisAlgorithm != null &&
                                        redisAlgorithm.equals(algorithmManager.getCurrentAlgorithmName()));

        return response;
    }

    @PutMapping("/algorithm")
    public ResponseEntity<Map<String, Object>> setAlgorithm(@RequestBody Map<String, String> request) {
        String algorithmName = request.get("algorithm");

        if (algorithmName == null || algorithmName.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Algorithm name is required")
            );
        }

        boolean success = algorithmManager.setCurrentAlgorithm(algorithmName);

        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("currentAlgorithm", algorithmManager.getCurrentAlgorithmName());
            response.put("description", algorithmManager.getCurrentAlgorithmDescription());
            response.put("message", "Algorithm successfully changed");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Unknown algorithm: " + algorithmName,
                    "availableAlgorithms", algorithmManager.getAvailableAlgorithms())
            );
        }
    }
    
    @GetMapping("/algorithms")
    public Map<String, Object> getAvailableAlgorithms() {
        Map<String, Object> response = new HashMap<>();
        response.put("algorithms", algorithmManager.getAlgorithmDescriptionsFromRedis());

        response.put("algorithmsFromRedis", algorithmManager.getAlgorithmDescriptionsFromRedis());

        return response;
    }
}
