package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.icon.IconService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/icon")
@RequiredArgsConstructor
public class IconController {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final IconService iconService;

    @GetMapping("/{category}/{id}")
    public ResponseEntity<byte[]> getIcon(@PathVariable String category, @PathVariable String id) {
        if (!SAFE_SEGMENT.matcher(category).matches() || !SAFE_SEGMENT.matcher(id).matches()) {
            return ResponseEntity.badRequest().build();
        }
        byte[] png = iconService.getIcon(category, id);
        if (png == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(png);
    }
}
