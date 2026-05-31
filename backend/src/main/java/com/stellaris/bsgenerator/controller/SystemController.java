package com.stellaris.bsgenerator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final ConfigurableApplicationContext context;

    @PostMapping("/shutdown")
    public ResponseEntity<Void> shutdown() {
        log.info("Graceful shutdown requested");
        // Respond first so the caller gets the 202 before we close the context.
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            log.info("Initiating Spring context shutdown");
            int code = SpringApplication.exit(context, () -> 0);
            System.exit(code);
        }, "graceful-shutdown").start();
        return ResponseEntity.accepted().build();
    }
}
