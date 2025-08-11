package com.meetingnotes.meetingnotestracker.controller;

import com.meetingnotes.meetingnotestracker.model.ActionItem;
import com.meetingnotes.meetingnotestracker.model.Meeting;
import com.meetingnotes.meetingnotestracker.repo.MeetingRepo;
import com.meetingnotes.meetingnotestracker.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/meetings")
public class MeetingController {

    private final MeetingRepo repo;
    private final AnalysisService analysis;

    public MeetingController(MeetingRepo repo, AnalysisService analysis) {
        this.repo = repo; this.analysis = analysis;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String title = String.valueOf(body.getOrDefault("title","Untitled Meeting"));
            String attendees = String.valueOf(body.getOrDefault("attendees",""));
            String transcript = String.valueOf(body.getOrDefault("transcript",""));
            Instant occurredAt = body.containsKey("occurredAt")
                    ? Instant.parse(String.valueOf(body.get("occurredAt")))
                    : Instant.now();

            Map<String,Object> analyzed = analysis.analyze(transcript);

            Meeting m = new Meeting();
            m.setTitle(title);
            m.setAttendees(attendees);
            m.setOccurredAt(occurredAt);
            m.setTranscript(transcript);
            m.setSummary(String.valueOf(analyzed.getOrDefault("summary","")));
            m.setDecisions(String.join("\n", (List<String>) analyzed.getOrDefault("decisions", List.of())));

            List<Map<String,String>> ai = (List<Map<String,String>>) analyzed.getOrDefault("actionItems", List.of());
            List<ActionItem> items = new ArrayList<>();
            for (Map<String,String> it : ai) {
                ActionItem a = new ActionItem();
                a.setOwner(it.getOrDefault("owner",""));
                a.setTask(it.getOrDefault("task",""));
                a.setDueDate(it.getOrDefault("due",""));
                a.setMeeting(m);
                items.add(a);
            }
            m.setActionItems(items);

            Meeting saved = repo.save(m);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<Meeting> list(@RequestParam(value="q", required=false) String q,
                              @RequestParam(value="attendee", required=false) String attendee) {
        if (q != null && !q.isBlank()) {
            return repo.findByTitleContainingIgnoreCaseOrTranscriptContainingIgnoreCase(q, q);
        }
        if (attendee != null && !attendee.isBlank()) {
            return repo.findByAttendeesContainingIgnoreCase(attendee);
        }
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (repo.existsById(id)) { repo.deleteById(id); return ResponseEntity.noContent().build(); }
        return ResponseEntity.notFound().build();
    }
}
