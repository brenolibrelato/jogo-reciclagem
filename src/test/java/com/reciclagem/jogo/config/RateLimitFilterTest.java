package com.reciclagem.jogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class RateLimitFilterTest {
    @Test void controlsHaveSeparateBudgetAndForwardedHeaderCannotBypassLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 60);
        for (int i = 0; i < 20; i++) assertThat(call(filter, "/api/cachoeira-lixo/id/comando", "fake" + i)).isEqualTo(200);
        assertThat(call(filter, "/api/ranking/cachoeira-lixo", "a")).isEqualTo(200);
        assertThat(call(filter, "/api/ranking/cachoeira-lixo", "b")).isEqualTo(200);
        assertThat(call(filter, "/api/ranking/cachoeira-lixo", "c")).isEqualTo(429);
    }
    int call(RateLimitFilter filter, String path, String forwarded) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.addHeader("X-Forwarded-For", forwarded);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}
