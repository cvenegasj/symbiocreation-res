package com.simbiocreacion.resource.dto;

public record IdeaAiResponse(String title, String description, boolean placeholder) {

    /** Constructor de conveniencia para ideas reales (placeholder = false). */
    public IdeaAiResponse(String title, String description) {
        this(title, description, false);
    }
}
