package com.jobradar.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PaginationParametersTest {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void createsPageableWithDefaultsAndAllowedSort() {
        PaginationParameters parameters = new PaginationParameters();

        assertThat(parameters.toPageable(Set.of("createdAt")))
                .isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void rejectsUnknownSortProperties() {
        PaginationParameters parameters = new PaginationParameters();
        parameters.setSort("passwordHash");

        assertThatThrownBy(() -> parameters.toPageable(Set.of("createdAt", "title")))
                .isInstanceOf(com.jobradar.common.exception.BadRequestException.class);
    }

    @Test
    void validationRejectsNegativePageAndExcessivePageSize() {
        PaginationParameters parameters = new PaginationParameters();
        parameters.setPage(-1);
        parameters.setSize(PaginationParameters.MAX_PAGE_SIZE + 1);

        assertThat(validator.validate(parameters)).hasSize(2);
    }

    @Test
    void validationRejectsInvalidDirection() {
        PaginationParameters parameters = new PaginationParameters();
        parameters.setDirection("SIDEWAYS");

        assertThat(validator.validate(parameters)).hasSize(1);
    }
}
