package com.dropboxstreamer.controller;

import com.dropboxstreamer.model.DropboxFile;
import com.dropboxstreamer.service.DropboxService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DropboxController {

    private final DropboxService dropboxService;

    public DropboxController(DropboxService dropboxService) {
        this.dropboxService = dropboxService;
    }

    @GetMapping("/files")
    public List<DropboxFile> listFiles(@RequestParam(defaultValue = "") String path) {
        return dropboxService.listFiles(path);
    }

    @GetMapping("/stream")
    public Map<String, String> getStreamUrl(@RequestParam String path) {
        String link = dropboxService.getTemporaryLink(path);
        return Map.of("link", link);
    }
}
