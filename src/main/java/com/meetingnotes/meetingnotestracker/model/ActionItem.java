package com.meetingnotes.meetingnotestracker.model;

import jakarta.persistence.*;

@Entity
public class ActionItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;
    @Lob private String task;
    private String dueDate; // free text: "before EOW", "Aug 15", etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    // getters/setters
    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public Meeting getMeeting() { return meeting; }
    public void setMeeting(Meeting meeting) { this.meeting = meeting; }
}
