package com.reciclagem.jogo.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/** Only a server-issued game identifier is accepted, never a client result. */
public class SubmeterPontuacaoRequest {
    private String sessionId;
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Campo não permitido: " + field);
    }
}
