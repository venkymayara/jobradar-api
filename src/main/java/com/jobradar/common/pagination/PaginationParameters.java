package com.jobradar.common.pagination;

import java.util.Set;

import com.jobradar.common.exception.BadRequestException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Validatable query parameters for endpoints that return a page of results. */
public class PaginationParameters {
    public static final int MAX_PAGE_SIZE = 100;

    @Min(0)
    @Max(1_000_000)
    private int page = 0;

    @Min(1)
    @Max(MAX_PAGE_SIZE)
    private int size = 20;

    @Pattern(regexp = "[A-Za-z][A-Za-z0-9]*")
    private String sort = "createdAt";

    @Pattern(regexp = "(?i)ASC|DESC")
    private String direction = "DESC";

    public Pageable toPageable(Set<String> allowedSortProperties) {
        if (!allowedSortProperties.contains(sort)) {
            throw new BadRequestException("Unsupported sort field: " + sort);
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
