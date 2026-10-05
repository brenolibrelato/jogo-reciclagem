package com.reciclagem.jogo.controller;

import com.reciclagem.jogo.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CachoeiraApiTest {
    MockMvc mvc;
    RankingService ranking;
    @BeforeEach void setup() {
        ranking = mock(RankingService.class);
        JogoService jogo = new JogoService(ranking, 15);
        mvc = MockMvcBuilders.standaloneSetup(new JogoApiController(jogo, ranking,
                new CachoeiraService(jogo, ranking))).build();
    }
    @Test void rejectsOriginalExploitAndForgedFieldsEvenWithSessionId() throws Exception {
        mvc.perform(post("/api/ranking/cachoeira-lixo").contentType("application/json")
                .content("{\"nomeJogador\":\"Fraude\",\"dificuldade\":\"DIFICIL\",\"pontuacao\":5000,\"tempoTotalMs\":0}"))
                .andExpect(status().isBadRequest());
        for (String field : new String[]{"pontuacao", "tempoTotalMs", "nomeJogador", "dificuldade"}) {
            mvc.perform(post("/api/ranking/cachoeira-lixo").contentType("application/json")
                    .content("{\"sessionId\":\"fake\",\"" + field + "\":0}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(ranking);
    }
    @Test void validatesOwnershipAndGameLifecycleThroughHttp() throws Exception {
        MockHttpSession owner = new MockHttpSession();
        String body = mvc.perform(post("/api/cachoeira-lixo/iniciar").session(owner).contentType("application/json")
                .content("{\"nomeJogador\":\"Ana\",\"dificuldade\":\"MEDIO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("jogando"))
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("sessionId").asText();
        mvc.perform(post("/api/ranking/cachoeira-lixo").contentType("application/json")
                .content("{\"sessionId\":\"" + id + "\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/ranking/cachoeira-lixo").session(owner).contentType("application/json")
                .content("{\"sessionId\":\"" + id + "\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/cachoeira-lixo/" + id + "/comando").session(owner).contentType("application/json")
                .content("{\"acao\":\"PAUSAR\",\"binX\":50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("pausado"));
        verifyNoInteractions(ranking);
    }
    @Test void missingBodyAndUnknownSessionFailClosed() throws Exception {
        mvc.perform(post("/api/ranking/cachoeira-lixo").contentType("application/json"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ranking/cachoeira-lixo").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(ranking);
    }
}
