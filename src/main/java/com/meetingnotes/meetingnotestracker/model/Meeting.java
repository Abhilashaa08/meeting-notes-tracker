package com.meetingnotes.meetingnotestracker.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Meeting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Instant occurredAt;
    private String attendees; // comma-separated for simplicity
    @Lob private String transcript;
    @Lob private String summary;
    @Lob private String decisions; // JSON string or newline text

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ActionItem> actionItems = new ArrayList<>();

    // getters/setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getAttendees() { return attendees; }
    public void setAttendees(String attendees) { this.attendees = attendees; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDecisions() { return decisions; }
    public void setDecisions(String decisions) { this.decisions = decisions; }
    public List<ActionItem> getActionItems() { return actionItems; }
    public void setActionItems(List<ActionItem> actionItems) { this.actionItems = actionItems; }
}
