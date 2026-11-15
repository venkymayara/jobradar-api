package com.jobradar.taxonomy.skill;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.pagination.PaginationParameters;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.List;

class SkillServiceTest {
    private final SkillRepository repository = mock(SkillRepository.class);
    private final SkillService service = new SkillService(repository);

    @Test void duplicateSkillIsRejectedCaseInsensitively() {
        when(repository.existsByNameIgnoreCase("Java")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(new SkillRequest("Java", null)));
    }

    @Test void searchUsesBoundedPageAndReturnsPageMetadata() {
        PaginationParameters parameters = new PaginationParameters();
        parameters.setPage(1); parameters.setSize(10); parameters.setSort("name"); parameters.setDirection("ASC");
        Pageable pageable = PageRequest.of(1, 10, Sort.by("name"));
        when(repository.browse("java", pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 12));
        var response = service.browse("java", parameters, false, false);
        assertEquals(1, response.page()); assertEquals(10, response.size()); assertEquals(12, response.totalElements());
        verify(repository).browse("java", pageable);
    }
}
