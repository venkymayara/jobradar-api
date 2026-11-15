package com.jobradar.taxonomy.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.jobradar.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;

class CategoryServiceTest {
    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryService service = new CategoryService(repository, mock(com.jobradar.taxonomy.role.RoleRepository.class));

    @Test void categoryHierarchyIsReturnedWithParentId() {
        CategoryEntity parent = mock(CategoryEntity.class);
        when(parent.getId()).thenReturn(8);
        when(parent.getName()).thenReturn("Technology");
        when(parent.isActive()).thenReturn(true);
        when(repository.browse(null, 8, PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "name"))))
                .thenReturn(new PageImpl<>(List.of(new CategoryEntity("Backend Development", "backend-development", parent, null, null))));
        var result = service.browse(null, 8, new com.jobradar.common.pagination.PaginationParameters(), false);
        assertEquals(8, result.content().getFirst().parentCategoryId());
    }

    @Test void updateRejectsCategoryCycle() {
        CategoryEntity current = mock(CategoryEntity.class);
        CategoryEntity ancestor = mock(CategoryEntity.class);
        when(repository.findById(1)).thenReturn(Optional.of(current));
        when(repository.findById(2)).thenReturn(Optional.of(ancestor));
        when(current.getId()).thenReturn(1);
        when(ancestor.getId()).thenReturn(2);
        when(ancestor.isActive()).thenReturn(true);
        when(ancestor.getParent()).thenReturn(current);
        when(current.getParent()).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.update(1,
                new CategoryRequest("Child", null, 2, null, null)));
    }
}
