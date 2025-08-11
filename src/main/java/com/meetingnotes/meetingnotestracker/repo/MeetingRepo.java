package com.meetingnotes.meetingnotestracker.repo;

import com.meetingnotes.meetingnotestracker.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepo extends JpaRepository<Meeting, Long> {
    List<Meeting> findByTitleContainingIgnoreCaseOrTranscriptContainingIgnoreCase(String title, String transcript);
    List<Meeting> findByAttendeesContainingIgnoreCase(String attendee);
}
