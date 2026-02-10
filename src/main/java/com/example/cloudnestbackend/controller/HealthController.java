package com.example.cloudnestbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public String health()
    {
        return "CloudNest Backend is UP 🚀";
    }

}
