package com.andrey.semeynikov.balanceronregistry.controller;

import com.andrey.semeynikov.balanceronregistry.model.ServiceInstance;
import com.andrey.semeynikov.balanceronregistry.service.RegistryService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class RegistryUiController {

    RegistryService registryService;

    @GetMapping("/ui/instances")
    public String showInstances(Model model) {
        List<ServiceInstance> instances = registryService.getActiveInstances();
        model.addAttribute("instances", instances);
        return "instances";
    }
}
