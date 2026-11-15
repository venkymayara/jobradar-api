package com.jobradar.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobradar.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsReturnSafeGenericResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/opportunities");
        var response = handler.handleUnexpected(new IllegalStateException("sensitive database detail"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiResponse<?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        assertThat(body.message()).doesNotContain("sensitive database detail");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void databaseConstraintErrorsReturnSafeConflictResponse() {
        var response = handler.handleConflict(
                new org.springframework.dao.DataIntegrityViolationException("secret SQL details"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("The request conflicts with existing data");
    }
}
