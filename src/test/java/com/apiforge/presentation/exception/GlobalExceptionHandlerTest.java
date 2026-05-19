package com.apiforge.presentation.exception;

import com.apiforge.domain.exception.SqlParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldHandleValidationExceptionAndReturn400() throws Exception {
        DummyRequest payload = new DummyRequest(""); // Blank field to trigger validation
        String jsonPayload = new ObjectMapper().writeValueAsString(payload);

        mockMvc.perform(post("/dummy/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://apiforge.com/errors/validation-failed")))
                .andExpect(jsonPath("$.title", is("Validation Failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("payload failed structural validation")))
                .andExpect(jsonPath("$.instance", is("/dummy/validate")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field", is("name")))
                .andExpect(jsonPath("$.errors[0].message", is("name cannot be blank")));
    }

    @Test
    void shouldHandleSqlParseExceptionAndReturn422() throws Exception {
        mockMvc.perform(post("/dummy/parse"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type", is("https://apiforge.com/errors/unprocessable-schema")))
                .andExpect(jsonPath("$.title", is("Unprocessable Schema")))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.detail", is("SQL schema syntax is invalid at line 2")))
                .andExpect(jsonPath("$.instance", is("/dummy/parse")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void shouldHandleGenericExceptionAndReturn500WithoutStacktrace() throws Exception {
        mockMvc.perform(post("/dummy/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type", is("https://apiforge.com/errors/internal-server-error")))
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.detail", is("An unexpected system error occurred. Please contact the administrator.")))
                .andExpect(jsonPath("$.instance", is("/dummy/generic")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.stackTrace").doesNotExist()) // Ensure NO stacktrace leaks
                .andExpect(jsonPath("$.cause").doesNotExist());
    }

    // Dummy Request DTO for testing validation
    public record DummyRequest(
        @NotBlank(message = "name cannot be blank")
        String name
    ) {}

    // Dummy Controller to trigger exceptions
    @RestController
    public static class DummyController {

        @PostMapping("/dummy/validate")
        public void testValidate(@Valid @RequestBody DummyRequest request) {
            // No-op
        }

        @PostMapping("/dummy/parse")
        public void testParse() {
            throw new SqlParseException("SQL schema syntax is invalid at line 2");
        }

        @PostMapping("/dummy/generic")
        public void testGeneric() {
            throw new RuntimeException("Sensitive database connection timeout details...");
        }
    }
}
