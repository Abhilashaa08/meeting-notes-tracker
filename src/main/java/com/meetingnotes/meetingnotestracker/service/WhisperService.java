package com.meetingnotes.meetingnotestracker.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WhisperService {
    public String transcribeAudio(MultipartFile file) {
        // Free mode: audio transcription disabled.
        // Use the web UI at / to capture speech -> text in the browser,
        // then click “Analyze” to get summary/decisions/action items.
        throw new UnsupportedOperationException("Audio transcription disabled in free mode. Use / (web UI) or POST /analyze.");
    }
}
