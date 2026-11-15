package com.jobradar.taxonomy.category;
import com.jobradar.common.api.*; import com.jobradar.common.pagination.PaginationParameters; import jakarta.validation.Valid;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController
public class CategoryController {
 private final CategoryService service; public CategoryController(CategoryService service){this.service=service;}
 @GetMapping("/api/v1/categories") public ApiResponse<PaginatedResponse<CategoryDto>> list(@RequestParam(required=false) String search,@RequestParam(required=false) Integer parentCategoryId,@Valid @ModelAttribute PaginationParameters page){return ApiResponse.success("Categories retrieved successfully",service.browse(search,parentCategoryId,page,false));}
 @GetMapping("/api/v1/admin/categories") public ApiResponse<PaginatedResponse<CategoryDto>> adminList(@RequestParam(required=false) String search,@RequestParam(required=false) Integer parentCategoryId,@Valid @ModelAttribute PaginationParameters page){return ApiResponse.success("Categories retrieved successfully",service.browse(search,parentCategoryId,page,true));}
 @PostMapping("/api/v1/admin/categories") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<CategoryDto> create(@Valid @RequestBody CategoryRequest r){return ApiResponse.success("Category created successfully",service.create(r));}
 @PutMapping("/api/v1/admin/categories/{id}") public ApiResponse<CategoryDto> update(@PathVariable Integer id,@Valid @RequestBody CategoryRequest r){return ApiResponse.success("Category updated successfully",service.update(id,r));}
 @PatchMapping("/api/v1/admin/categories/{id}/activate") public ApiResponse<Void> activate(@PathVariable Integer id){service.activate(id);return ApiResponse.success("Category activated successfully",null);}
 @PatchMapping("/api/v1/admin/categories/{id}/deactivate") public ApiResponse<Void> deactivate(@PathVariable Integer id){service.deactivate(id);return ApiResponse.success("Category deactivated successfully",null);}
}
