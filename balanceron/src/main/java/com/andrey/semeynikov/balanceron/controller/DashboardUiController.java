package com.andrey.semeynikov.balanceron.controller;

import static lombok.AccessLevel.PRIVATE;

import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import com.andrey.semeynikov.balanceron.service.AlgorithmManager;
import com.andrey.semeynikov.balanceron.service.InstanceRegistryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequestMapping("/ui/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DashboardUiController {

  InstanceRegistryService registryService;
  AlgorithmManager algorithmManager;

  @GetMapping
  public Mono<String> showInstances(Model model) {
    List<ServiceInstance> instances = registryService.getActiveInstances();
    model.addAttribute("instances", instances);
    model.addAttribute("currentAlgorithm", algorithmManager.getCurrentAlgorithmName());
    model.addAttribute("algorithmDescription", algorithmManager.getCurrentAlgorithmDescription());
    model.addAttribute("algorithms", algorithmManager.getAlgorithmDescriptionsFromRedis());
    return Mono.just("balanceron");
  }

  @PostMapping("/algorithm")
  public Mono<String> changeAlgorithm(WebSession session, ServerWebExchange exchange) {

    log.info("Request to update algorithm");

    return exchange
        .getFormData()
        .map(formData -> formData.getFirst("algorithm"))
        .map(
            algorithm -> {

              boolean success = algorithmManager.setCurrentAlgorithm(algorithm);
              if (success) {
                session
                    .getAttributes()
                    .put(
                        "successMessage",
                        "Algorithm changed to " + algorithm + " and saved to Redis");
              } else {
                session.getAttributes().put("errorMessage", "Unknown algorithm: " + algorithm);
              }

              return "redirect:/ui/dashboard";
            });
  }
}
