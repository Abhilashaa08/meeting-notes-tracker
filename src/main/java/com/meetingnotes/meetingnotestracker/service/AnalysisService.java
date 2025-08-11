package com.meetingnotes.meetingnotestracker.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnalysisService {

    // Avoid using these as "owners" or as meaningful sentence starters
    private static final Set<String> STOP_LEADS = Set.of(
            "we","i","hi","hello","hey",
            "today","tomorrow","yesterday",
            "lets","let's","this","that","there","here","discussion",
            "and","but","then","so","also","ok","okay",
            // common verbs/nouns that get capitalized at sentence start
            "want","needs","need","required","require","request","asks","ask","asking",
            "assign","assigned","decide","decided","agree","agreed"
    );

    private static final String[] WEEKDAYS = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
    private static final String[] MONTHS   = {"january","february","march","april","may","june","july","august","september","october","november","december"};

    private boolean startsWithStopLead(String sentenceLower) {
        String[] parts = sentenceLower.stripLeading().split("\\s+");
        if (parts.length == 0) return false;
        String first = parts[0];
        if (STOP_LEADS.contains(first)) return true;
        for (String d : WEEKDAYS) if (first.equals(d)) return true;
        for (String m : MONTHS)  if (first.equals(m))  return true;
        return false;
    }

    public Map<String, Object> analyze(String transcript) {
        String cleaned = transcript == null ? "" : transcript.trim();
        List<String> sentences = splitSentences(cleaned);

        List<String> summary = pickSummary(sentences, 3);
        List<String> decisions = extractDecisions(sentences);
        List<Map<String, String>> actionItems = extractActions(sentences);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", String.join(" ", summary));
        out.put("decisions", decisions);
        out.put("actionItems", actionItems);
        return out;
    }

    /** Split on ., !, ?, and also on comma when followed by capitalized conjunctions (And/But/So/Then/Also/I/We). */
    private List<String> splitSentences(String text) {
        if (text.isEmpty()) return List.of();

        // First split on end punctuation
        List<String> prelim = new ArrayList<>();
        String[] parts = text.split("(?<=[.!?])\\s+");
        if (parts.length == 1) {
            prelim.add(text.trim());
        } else {
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) prelim.add(t);
            }
        }

        // Then split further on ", <Conjunction...>" starts
        List<String> finalList = new ArrayList<>();
        Pattern commaConj = Pattern.compile("\\s*,\\s+(?=(And|But|So|Then|Also|I|We)\\b)");
        for (String s : prelim) {
            String[] subs = commaConj.split(s);
            for (String sub : subs) {
                String t = sub.trim();
                if (!t.isEmpty()) finalList.add(t);
            }
        }

        return finalList.isEmpty() ? List.of(text.trim()) : finalList;
    }

    private List<String> pickSummary(List<String> sentences, int max) {
        if (sentences.isEmpty()) return List.of();
        String[] keywords = {"goal","scope","timeline","deadline","risk","blocker","plan","decide","decision",
                "agree","deliver","owner","next","milestone","priority","kpi","backend","development","project"};
        Map<String,Integer> score = new HashMap<>();
        for (String s : sentences) {
            String l = s.toLowerCase();
            int sc = 0;
            for (String k : keywords) if (l.contains(k)) sc++;
            sc += Math.min(2, l.length()/120); // tiny bump for longer sentences
            score.put(s, sc);
        }
        return sentences.stream()
                .sorted((a,b) -> Integer.compare(score.getOrDefault(b,0), score.getOrDefault(a,0)))
                .limit(max)
                .toList();
    }

    private List<String> extractDecisions(List<String> sentences) {
        String[] keys = {"decided","decision","approve","approved","agree","agreed","choose","chose","selected","finalize","finalized"};
        List<String> out = new ArrayList<>();
        for (String s : sentences) {
            String l = s.toLowerCase();
            for (String k : keys) {
                if (l.contains(k)) { out.add(s); break; }
            }
        }
        return out;
    }

    private List<Map<String,String>> extractActions(List<String> sentences) {
        // ---- Patterns ----
        // NOTE: removed "|to" here to prevent "Want to" or "Need to" treating "Want"/"Need" as a name.
        Pattern nameWillVerb = Pattern.compile(
                "\\b([A-Z][a-zA-Z]+)\\b\\s+(will|should|must)\\s+([a-z]+)\\b([^.]*)",
                Pattern.CASE_INSENSITIVE
        );

        // 2) "assign(ing/ed) <what> to <Name> ..."
        Pattern assigningToName = Pattern.compile(
                "\\bassign(?:ing|ed)?\\s+(.+?)\\s+to\\s+([A-Z][a-zA-Z]+)\\b([^.]*)",
                Pattern.CASE_INSENSITIVE
        );

        // 3) "I/We/Please ask/Ask/Need/Want <Name> to <verb> ..."
        Pattern wantNeedAskPattern = Pattern.compile(
                "\\b(?:I|We|Please\\s+ask|Ask|Asked|Request|Requested|Require|Required|Need|Needs|Want|Wants)\\b.*?\\b([A-Z][a-zA-Z]+)\\b\\s+to\\s+([a-z]+)\\b([^.]*)",
                Pattern.CASE_INSENSITIVE
        );

        // Due date hints
        Pattern due = Pattern.compile(
                "\\b(by|before|on)\\s+([A-Za-z]+\\s?\\d{1,2}|\\d{1,2}/\\d{1,2}|tomorrow|today|next\\s+week|EOW|EOD)\\b",
                Pattern.CASE_INSENSITIVE
        );

        List<Map<String,String>> items = new ArrayList<>();

        // Track concise "task context" from prior sentences
        String lastTaskContext = "";

        for (String s : sentences) {
            String sTrim = s == null ? "" : s.trim();
            if (sTrim.isEmpty()) continue;
            String sLower = sTrim.toLowerCase();

            // Update context if this sentence names a task/work/project
            if (looksLikeTaskContext(sLower)) {
                lastTaskContext = extractConciseContext(sTrim);
            }

            // 2) assigning … to <Name> …
            Matcher mAssign = assigningToName.matcher(sTrim);
            if (mAssign.find()) {
                String what = mAssign.group(1).trim();
                String owner = cap(mAssign.group(2));
                if (isStopWord(owner)) { /* skip bogus owners */ }
                else {
                    String tail  = mAssign.group(3) != null ? mAssign.group(3).trim() : "";
                    String task  = ("take ownership of " + what + (tail.isEmpty() ? "" : " " + tail)).trim();
                    task = stripEndPunct(task);
                    task = resolvePronouns(task, lastTaskContext);

                    String dueDate = "";
                    Matcher d = due.matcher(sTrim);
                    if (d.find()) dueDate = d.group(0);

                    items.add(makeItem(owner, task, dueDate));
                }
            }

            // 3) I/We/Ask/Need/Want <Name> to <verb> …
            Matcher mWant = wantNeedAskPattern.matcher(sTrim);
            if (mWant.find()) {
                String owner = cap(mWant.group(1));
                if (!isStopWord(owner)) {
                    String verb  = mWant.group(2).toLowerCase();
                    String rest  = mWant.group(3) != null ? mWant.group(3).trim() : "";
                    rest = sanitizeRest(rest, owner);
                    String task  = (verb + " " + rest).trim();
                    task = stripEndPunct(task);
                    task = resolvePronouns(task, lastTaskContext);

                    String dueDate = "";
                    Matcher d = due.matcher(sTrim);
                    if (d.find()) dueDate = d.group(0);

                    items.add(makeItem(owner, task, dueDate));
                }
            }

            // If the sentence starts with a stop-lead (e.g., "Today", "And"), skip owner-at-start heuristics
            if (startsWithStopLead(sLower)) {
                continue;
            }

            // 1) <Name> will/should/must <verb> …
            Matcher m1 = nameWillVerb.matcher(sTrim);
            if (m1.find()) {
                String owner = cap(m1.group(1));
                if (!isStopWord(owner)) {
                    String verb  = m1.group(3).toLowerCase();
                    String rest  = m1.group(4) != null ? m1.group(4).trim() : "";
                    rest = sanitizeRest(rest, owner);
                    String task  = (verb + " " + rest).trim();
                    task = stripEndPunct(task);
                    task = resolvePronouns(task, lastTaskContext);

                    String dueDate = "";
                    Matcher d = due.matcher(sTrim);
                    if (d.find()) dueDate = d.group(0);

                    items.add(makeItem(owner, task, dueDate));
                }
            }
        }
        return items;
    }

    // -------- helpers --------

    private boolean looksLikeTaskContext(String sLower) {
        String[] cues = {"task","work","project","feature","ticket","backlog","story","bug","issue",
                "api","endpoint","spec","document","doc","design","migration","deployment","pipeline",
                "monitoring","dashboard","backend","frontend","database","schema","index","test plan",
                "testcase","configuration","development"};
        for (String c : cues) if (sLower.contains(c)) return true;
        return false;
    }

    /** Extract a concise phrase for context (e.g., "backend development work") from a longer sentence. */
    private String extractConciseContext(String sentence) {
        // Try to capture nouny phrase around common keywords
        Pattern p = Pattern.compile("(?i)(backend|frontend|api|project|feature|task|work|development|migration)\\b([^.,;]*)");
        Matcher m = p.matcher(sentence);
        if (m.find()) {
            String head = m.group(1);
            String tail = m.group(2) != null ? m.group(2) : "";
            String ctx  = (head + tail).replaceAll("\\b(we|i|have|has|some|the|our)\\b", "").replaceAll("\\s{2,}", " ").trim();
            return ctx.isEmpty() ? (head + " " + tail).trim() : ctx;
        }
        // Fallback: last ~6 words
        String[] toks = sentence.replaceAll("[.?!]", "").trim().split("\\s+");
        int from = Math.max(0, toks.length - 6);
        return String.join(" ", Arrays.copyOfRange(toks, from, toks.length)).trim();
    }

    /** Remove leading echoes/fillers from the captured rest-of-task. */
    private String sanitizeRest(String rest, String owner) {
        if (rest == null) return "";
        String r = rest.trim();

        // Strip leading conjunctions/fillers
        r = r.replaceFirst("(?i)^(and|but|so|then|also)\\s+", "");

        // Remove any accidental echo like "And I want Nelson to ..." or "I want <owner> to ..."
        String ownerEsc = Pattern.quote(owner);
        r = r.replaceFirst("(?i)^i\\s+want\\s+" + ownerEsc + "\\s+to\\s+[a-z]+\\b\\s*", "");
        r = r.replaceFirst("(?i)^and\\s+i\\s+want\\s+" + ownerEsc + "\\s+to\\s+[a-z]+\\b\\s*", "");
        r = r.replaceFirst("(?i)^we\\s+(need|want|ask|asked|request|requested|require|required)\\s+" + ownerEsc + "\\s+to\\s+[a-z]+\\b\\s*", "");
        return r.trim();
    }

    private String resolvePronouns(String phrase, String context) {
        if (phrase == null || phrase.isEmpty()) return phrase;
        if (context == null || context.isEmpty()) return phrase;

        String ctx = context;
        String out = phrase
                .replaceAll("(?i)\\bpick\\s+it\\s+up\\b", "pick up " + ctx)
                .replaceAll("(?i)\\btake\\s+it\\s+up\\b", "take up " + ctx)
                .replaceAll("(?i)\\bwork\\s+on\\s+it\\b", "work on " + ctx)
                .replaceAll("(?i)\\bhandle\\s+it\\b", "handle " + ctx)
                .replaceAll("(?i)\\bthis\\s+task\\b", ctx)
                .replaceAll("(?i)\\bthis\\s+work\\b", ctx)
                .replaceAll("(?i)\\bthe\\s+task\\b", ctx)
                .replaceAll("(?i)\\bthe\\s+work\\b", ctx)
                .replaceAll("(?i)\\bit\\b", ctx); // generic last
        return out.replaceAll("\\s{2,}", " ").trim();
    }

    private String stripEndPunct(String s) {
        return s == null ? "" : s.replaceAll("[.?!]+$", "").trim();
    }

    private Map<String,String> makeItem(String owner, String task, String due) {
        Map<String,String> it = new LinkedHashMap<>();
        it.put("owner", owner);
        it.put("task", task);
        it.put("due", (due == null) ? "" : due);
        return it;
    }

    private boolean isStopWord(String token) {
        if (token == null) return true;
        String t = token.toLowerCase();
        if (STOP_LEADS.contains(t)) return true;
        for (String d : WEEKDAYS) if (t.equals(d)) return true;
        for (String m : MONTHS)   if (t.equals(m))  return true;
        return false;
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
