package com.manil.manil.describe;

import java.util.List;

public class DescribeDto {
    public static class DescribeRequest {
        private String hint;   // 사용자 한줄 의도
        private String locale; // "ko" 기본

        public String getHint() { return hint; }
        public void setHint(String hint) { this.hint = hint; }
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
    }

    public static class DescribeResponse {
        private String headline;
        private List<String> pros;
        private List<String> cons;
        private String regionalNotes;
        private List<String> useCases;
        private String fullText;

        public String getHeadline() { return headline; }
        public void setHeadline(String headline) { this.headline = headline; }
        public List<String> getPros() { return pros; }
        public void setPros(List<String> pros) { this.pros = pros; }
        public List<String> getCons() { return cons; }
        public void setCons(List<String> cons) { this.cons = cons; }
        public String getRegionalNotes() { return regionalNotes; }
        public void setRegionalNotes(String regionalNotes) { this.regionalNotes = regionalNotes; }
        public List<String> getUseCases() { return useCases; }
        public void setUseCases(List<String> useCases) { this.useCases = useCases; }
        public String getFullText() { return fullText; }
        public void setFullText(String fullText) { this.fullText = fullText; }
    }
}
