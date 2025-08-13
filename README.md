
# Meeting Notes Tracker

## Overview

Meeting Notes Tracker is a web-based application that enables users to record, transcribe, and organize meeting discussions in real time.  
The application uses a **speech-to-text API** to capture spoken input, securely stores transcripts in a database, and provides a web-based interface to search and review meeting history.

The goal of this project is to eliminate manual note-taking, improve accuracy, and make meeting documentation instantly searchable for future reference.

---

## Key Features

- **Real-time transcription** using an integrated speech-to-text API.  
- **Secure storage** of meeting transcripts in a persistent PostgreSQL database.  
- **Search and filter** functionality to locate relevant meeting notes quickly.  
- **User-friendly web interface** built with HTML, CSS, and JavaScript, served via Spring Boot.  
- **Backend API endpoints** for transcript storage, retrieval, and management.  

---

## Technology Stack

**Backend:** Java 17, Spring Boot  
**Database:** PostgreSQL  
**Speech-to-Text Service:** Deepgram API (or equivalent)  
**Frontend:** HTML, CSS, JavaScript (served as static resources in Spring Boot)  
**Build Tool:** Maven  

---

## System Architecture

```plaintext
User Speech Input
        ↓
Speech-to-Text API — Real-time Transcription
        ↓
Spring Boot Backend — Processing and Storage
        ↓
PostgreSQL Database
        ↓
Frontend — Search and Display of Notes
````

---

## Setup Instructions

### Prerequisites

* Java 17 installed
* PostgreSQL running locally or on a remote server
* Maven installed
* API key for the speech-to-text service

### Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/<your-username>/meeting-notes-tracker.git
   cd meeting-notes-tracker
   ```

2. **Configure environment variables**
   Create a `.env` file in the project root (or set these in your IDE/OS):

   ```ini
   SPEECH_TO_TEXT_API_KEY=your-api-key
   DATABASE_URL=jdbc:postgresql://localhost:5432/yourdb
   DATABASE_USER=your-db-user
   DATABASE_PASSWORD=your-db-password
   ```

3. **Build the application**

   ```bash
   mvn clean install
   ```

4. **Run the application**

   ```bash
   mvn spring-boot:run
   ```

5. **Access in browser**
   Open:

   ```plaintext
   http://localhost:8080
   ```

---

## Example Use Case

**During a project meeting:**

* The application listens to the discussion in real time.
* Speech is converted to text using the configured transcription service.
* The transcript is stored in the database along with the meeting title and timestamp.

**Later:**

* A user searches for a keyword, e.g., `"deployment timeline"`.
* The system returns all meeting notes containing that keyword and highlights the relevant sections.

---

## Notes

* Environment variables can be set via a `.env` file, your IDE run configuration, or your shell environment.
* If you use a different speech-to-text provider, update the API client configuration accordingly.
* Default server port is **8080** (Spring Boot). Adjust via `application.properties` if needed.

---

## Future Enhancements

* Implement **user authentication** and **role-based access control** for secure transcript access.
* Add support for **tagging and categorizing** meeting notes.
* Provide an **export** option (PDF/Word) for sharing or archiving.
* Integrate with **calendar applications** to automatically track and log meeting details.

```

