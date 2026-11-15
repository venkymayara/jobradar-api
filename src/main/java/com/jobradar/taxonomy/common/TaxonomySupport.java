package com.jobradar.taxonomy.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import com.jobradar.common.exception.BadRequestException;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.pagination.PaginationParameters;
import org.springframework.data.domain.Pageable;

public final class TaxonomySupport {
    private TaxonomySupport() {}
    public static String slug(String input) {
        String value = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (value.isBlank()) throw new BadRequestException("A usable name is required");
        return value;
    }
    public static Pageable pageable(PaginationParameters p, Set<String> allowed, String defaultSort) {
        String sort = p.getSort().equals("createdAt") ? defaultSort : p.getSort();
        if (!allowed.contains(sort)) throw new BadRequestException("Unsupported sort field: " + sort);
        return org.springframework.data.domain.PageRequest.of(p.getPage(), p.getSize(),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.fromString(p.getDirection()), sort));
    }
    public static void duplicate(boolean duplicate, String kind) {
        if (duplicate) throw new ConflictException(kind + " already exists");
    }
}
