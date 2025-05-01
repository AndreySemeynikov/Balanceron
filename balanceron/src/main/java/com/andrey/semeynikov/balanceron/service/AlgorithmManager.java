package com.andrey.semeynikov.balanceron.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlgorithmManager {

  private final Map<String, LoadBalancingAlgorithm> algorithmMap = new HashMap<>();
  @Getter private String currentAlgorithmName;

  private static final String REDIS_CURRENT_ALGORITHM_KEY = "balanceron:current_algorithm";

  @Value("${balanceron.loadbalancing.defaultAlgorithm:Round Robin}")
  private String defaultAlgorithm;

  private final StringRedisTemplate redisTemplate;

  public AlgorithmManager(
      List<LoadBalancingAlgorithm> algorithms, StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;

    algorithms.forEach(
        algorithm -> {
          algorithmMap.put(algorithm.getName(), algorithm);
          log.info("Registered load balancing algorithm: {}", algorithm.getName());
          saveAlgorithmToRedis(algorithm);
        });
    String savedAlgorithm = redisTemplate.opsForValue().get(REDIS_CURRENT_ALGORITHM_KEY);

    if (savedAlgorithm != null && algorithmMap.containsKey(savedAlgorithm)) {
      currentAlgorithmName = savedAlgorithm;
      log.info("Loaded algorithm from Redis: {}", currentAlgorithmName);
    } else {
      // Используем алгоритм по умолчанию, если нет сохраненного
      currentAlgorithmName = defaultAlgorithm;

      // Проверяем, что алгоритм по умолчанию существует
      if (!algorithmMap.containsKey(currentAlgorithmName)) {
        if (!algorithmMap.isEmpty()) {
          currentAlgorithmName = algorithmMap.keySet().iterator().next();
          log.warn(
              "Default algorithm '{}' not found, using '{}' instead",
              defaultAlgorithm,
              currentAlgorithmName);
        } else {
          log.error("No load balancing algorithms registered!");
          throw new IllegalStateException("No load balancing algorithms available");
        }
      }

      // Сохраняем выбранный алгоритм в Redis
      saveCurrentAlgorithmToRedis();
    }

    log.info("Using load balancing algorithm: {}", currentAlgorithmName);
  }

  private void saveAlgorithmToRedis(LoadBalancingAlgorithm algorithm) {
    String key = "balanceron:algorithms:" + algorithm.getName();
    redisTemplate.opsForHash().put(key, "name", algorithm.getName());
    redisTemplate.opsForHash().put(key, "description", algorithm.getDescription());
  }

  private void saveCurrentAlgorithmToRedis() {
    redisTemplate.opsForValue().set(REDIS_CURRENT_ALGORITHM_KEY, currentAlgorithmName);
    log.debug("Saved current algorithm to Redis: {}", currentAlgorithmName);
  }

  public LoadBalancingAlgorithm getCurrentAlgorithm() {
    return algorithmMap.get(currentAlgorithmName);
  }

  public boolean setCurrentAlgorithm(String algorithmName) {
    if (algorithmMap.containsKey(algorithmName)) {
      currentAlgorithmName = algorithmName;
      saveCurrentAlgorithmToRedis();
      log.info("Switched to load balancing algorithm: {}", algorithmName);
      return true;
    } else {
      log.warn("Attempted to switch to unknown algorithm: {}", algorithmName);
      return false;
    }
  }

  public List<String> getAvailableAlgorithms() {
    return algorithmMap.keySet().stream().sorted().toList();
  }

  public String getCurrentAlgorithmDescription() {
    LoadBalancingAlgorithm algorithm = getCurrentAlgorithm();
    return algorithm != null ? algorithm.getDescription() : "";
  }

  public Map<String, String> getAlgorithmDescriptionsFromRedis() {
    Map<String, String> descriptions = new HashMap<>();

    Set<String> keys = redisTemplate.keys("balanceron:algorithms:*");
    for (String key : keys) {
      Map<Object, Object> algorithmData = redisTemplate.opsForHash().entries(key);
      String name = (String) algorithmData.get("name");
      String description = (String) algorithmData.get("description");
      if (name != null) {
        descriptions.put(name, description);
      }
    }

    return descriptions;
  }
}
