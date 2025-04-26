package com.andrey.semeynikov.balanceron.service;


import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Сервис для управления реестром инстансов сервисов. Обеспечивает регистрацию, обновление heartbeat
 * и получение активных инстансов.
 */
@Service
@Slf4j
public class InstanceRegistryService {

  // Хранилище инстансов с потокобезопасной реализацией
  private final Map<String, ServiceInstance> instances = new ConcurrentHashMap<>();

  // Время в секундах, после которого инстанс считается неактивным
  private static final int HEARTBEAT_TIMEOUT_SECONDS = 30;

  /**
   * Регистрирует новый инстанс сервиса или обновляет существующий
   *
   * @param instance Инстанс сервиса для регистрации
   */
  public void registerInstance(ServiceInstance instance) {
    instance.setLastHeartbeat(Instant.now());
    instances.put(instance.getId(), instance);
    log.info(
        "Registered instance: ID={}, Service={}, Host={}:{}",
        instance.getId(),
        instance.getServiceName(),
        instance.getHost(),
        instance.getPort());
  }

  /**
   * Обновляет время последнего heartbeat для указанного инстанса
   *
   * @param instanceId ID инстанса
   * @return true если инстанс найден и обновлен, false в противном случае
   */
  public boolean updateHeartbeat(String instanceId) {
    ServiceInstance instance = instances.get(instanceId);
    if (instance != null) {
      instance.setLastHeartbeat(Instant.now());
      log.debug("Updated heartbeat for instance: {}", instanceId);
      return true;
    } else {
      log.warn("Attempted to update heartbeat for unknown instance: {}", instanceId);
      return false;
    }
  }

  /**
   * Возвращает список всех активных инстансов
   *
   * @return Список активных инстансов
   */
  public List<ServiceInstance> getActiveInstances() {
    Instant threshold = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

    return instances.values().stream()
        .filter(instance -> instance.getLastHeartbeat().isAfter(threshold))
        .collect(Collectors.toList());
  }

  /**
   * Возвращает список всех активных инстансов указанного сервиса
   *
   * @param serviceName Имя сервиса
   * @return Список активных инстансов указанного сервиса
   */
  public List<ServiceInstance> getActiveInstancesByService(String serviceName) {
    Instant threshold = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

    return instances.values().stream()
        .filter(
            instance ->
                instance.getServiceName().equals(serviceName)
                    && instance.getLastHeartbeat().isAfter(threshold))
        .collect(Collectors.toList());
  }

  /**
   * Удаляет инстанс из реестра
   *
   * @param instanceId ID инстанса для удаления
   * @return true если инстанс был удален, false если инстанс не найден
   */
  public boolean removeInstance(String instanceId) {
    ServiceInstance removed = instances.remove(instanceId);
    if (removed != null) {
      log.info("Removed instance: {}", instanceId);
      return true;
    } else {
      log.warn("Attempted to remove unknown instance: {}", instanceId);
      return false;
    }
  }

  /**
   * Получает инстанс по ID
   *
   * @param instanceId ID инстанса
   * @return Инстанс или null, если не найден
   */
  public ServiceInstance getInstance(String instanceId) {
    return instances.get(instanceId);
  }

  /**
   * Возвращает количество зарегистрированных инстансов
   *
   * @return Количество инстансов
   */
  public int getInstanceCount() {
    return instances.size();
  }

  /**
   * Возвращает количество активных инстансов
   *
   * @return Количество активных инстансов
   */
  public int getActiveInstanceCount() {
    return getActiveInstances().size();
  }

  /** Периодически очищает неактивные инстансы Запускается каждые 30 секунд */
  @Scheduled(fixedRate = 30000)
  public void cleanupInactiveInstances() {
    Instant threshold = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

    List<String> inactiveIds = new ArrayList<>();

    // Собираем ID неактивных инстансов
    instances.forEach(
        (id, instance) -> {
          if (instance.getLastHeartbeat().isBefore(threshold)) {
            inactiveIds.add(id);
          }
        });

    // Удаляем неактивные инстансы
    if (!inactiveIds.isEmpty()) {
      inactiveIds.forEach(instances::remove);
      log.info("Cleaned up {} inactive instances", inactiveIds.size());
    }
  }

  /**
   * Обновляет вес инстанса для алгоритма Weighted Round Robin
   *
   * @param instanceId ID инстанса
   * @param weight Новый вес
   * @return true если инстанс найден и обновлен, false в противном случае
   */
  public boolean updateInstanceWeight(String instanceId, int weight) {
    if (weight <= 0) {
      throw new IllegalArgumentException("Weight must be greater than 0");
    }

    ServiceInstance instance = instances.get(instanceId);
    if (instance != null) {
      instance.setWeight(weight);
      log.info("Updated weight for instance {}: new weight = {}", instanceId, weight);
      return true;
    } else {
      log.warn("Attempted to update weight for unknown instance: {}", instanceId);
      return false;
    }
  }
}
