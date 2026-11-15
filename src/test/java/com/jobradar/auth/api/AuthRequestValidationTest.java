package com.jobradar.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuthRequestValidationTest {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void registrationRejectsMalformedEmail() {
        RegisterRequest request = new RegisterRequest("Test Person", "not-an-email", "GoodPassword123!");

        assertThat(validator.validate(request)).extracting("propertyPath")
                .anySatisfy(path -> assertThat(path.toString()).isEqualTo("email"));
    }

    @Test
    void registrationEnforcesPasswordPolicy() {
        RegisterRequest request = new RegisterRequest("Test Person", "test@example.com", "weakpassword");

        assertThat(validator.validate(request)).extracting("propertyPath")
                .anySatisfy(path -> assertThat(path.toString()).isEqualTo("password"));
    }
}
