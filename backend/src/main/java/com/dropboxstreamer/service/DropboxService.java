package com.dropboxstreamer.service;

import com.dropboxstreamer.model.DropboxFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DropboxService {

    private static final String DROPBOX_API = "https://api.dropboxapi.com/2";

    @Value("${dropbox.access-token}")
    private String accessToken;

    private final RestTemplate restTemplate;

    public DropboxService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<DropboxFile> listFiles(String path) {
        String normalizedPath = (path == null || path.equals("/") || path.isBlank()) ? "" : path;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "path", normalizedPath,
                "recursive", false,
                "include_deleted", false
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                DROPBOX_API + "/files/list_folder",
                request,
                Map.class
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) response.getBody().get("entries");

        return entries.stream()
                .filter(e -> {
                    String tag = (String) e.get(".tag");
                    if ("folder".equals(tag)) return true;
                    if ("file".equals(tag)) {
                        String name = (String) e.get("name");
                        return name != null && name.toLowerCase().endsWith(".mp3");
                    }
                    return false;
                })
                .map(e -> {
                    String tag = (String) e.get(".tag");
                    String name = (String) e.get("name");
                    String filePath = (String) e.get("path_display");
                    Long size = e.get("size") != null ? ((Number) e.get("size")).longValue() : null;
                    return new DropboxFile(name, filePath, tag, size);
                })
                .sorted(Comparator.comparing(f -> "folder".equals(f.type()) ? 0 : 1))
                .collect(Collectors.toList());
    }

    public String getTemporaryLink(String path) {
        if (path == null || !path.toLowerCase().endsWith(".mp3")) {
            throw new IllegalArgumentException("Only MP3 files are allowed");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("path", path);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                DROPBOX_API + "/files/get_temporary_link",
                request,
                Map.class
        );

        return (String) response.getBody().get("link");
    }
}
