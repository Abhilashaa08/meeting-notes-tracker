package com.meetingnotes.meetingnotestracker.controller;

import com.meetingnotes.meetingnotestracker.service.WhisperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class MeetingNotesController {

    private final WhisperService whisperService;

    public MeetingNotesController(WhisperService whisperService) {
        this.whisperService = whisperService;
    }

    @PostMapping("/upload-audio")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String transcript = whisperService.transcribeAudio(file);
            return ResponseEntity.ok(Map.of("transcript", transcript));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
