package com.jobradar.taxonomy.category;
import java.util.*; import java.util.stream.*;
import com.jobradar.common.api.PaginatedResponse; import com.jobradar.common.exception.*; import com.jobradar.common.pagination.PaginationParameters; import com.jobradar.taxonomy.common.TaxonomySupport;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class CategoryService {
 private static final Set<String> SORTS=Set.of("name","slug","opportunityCount","createdAt"); private final CategoryRepository repo; private final com.jobradar.taxonomy.role.RoleRepository roles;
 public CategoryService(CategoryRepository repo,com.jobradar.taxonomy.role.RoleRepository roles){this.repo=repo;this.roles=roles;}
 public PaginatedResponse<CategoryDto> browse(String search,Integer parent,PaginationParameters p,boolean admin){Page<CategoryDto> result=(admin?repo.adminBrowse(blank(search),parent,TaxonomySupport.pageable(p,SORTS,"name")):repo.browse(blank(search),parent,TaxonomySupport.pageable(p,SORTS,"name"))).map(CategoryDto::from);return PaginatedResponse.from(result);}
 private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
 @Transactional public CategoryDto create(CategoryRequest r){String n=r.categoryName().trim(),s=r.slug()==null||r.slug().isBlank()?TaxonomySupport.slug(n):TaxonomySupport.slug(r.slug());TaxonomySupport.duplicate(repo.existsByNameIgnoreCase(n)||repo.existsBySlugIgnoreCase(s),"Category"); return CategoryDto.from(repo.save(new CategoryEntity(n,s,parent(r.parentCategoryId()),r.description(),r.icon())));}
 @Transactional public CategoryDto update(Integer id,CategoryRequest r){CategoryEntity e=get(id);String n=r.categoryName().trim(),s=r.slug()==null||r.slug().isBlank()?TaxonomySupport.slug(n):TaxonomySupport.slug(r.slug());TaxonomySupport.duplicate(repo.existsByNameIgnoreCaseAndIdNot(n,id)||repo.existsBySlugIgnoreCaseAndIdNot(s,id),"Category");CategoryEntity p=parent(r.parentCategoryId());if(p!=null&&(p.getId().equals(id)||ancestorContains(p,id)))throw new BadRequestException("Category hierarchy cannot contain a cycle");e.update(n,s,p,r.description(),r.icon());return CategoryDto.from(e);}
 private boolean ancestorContains(CategoryEntity c,Integer id){for(CategoryEntity x=c;x!=null;x=x.getParent())if(x.getId().equals(id))return true;return false;}
 private CategoryEntity parent(Integer id){if(id==null)return null;CategoryEntity p=get(id);if(!p.isActive())throw new BadRequestException("Parent category must be active");return p;}
 private CategoryEntity get(Integer id){return repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Category not found"));}
 @Transactional public void activate(Integer id){CategoryEntity e=get(id);if(e.getParent()!=null&&!e.getParent().isActive())throw new BadRequestException("Parent category must be active");e.setActive(true);}
 @Transactional public void deactivate(Integer id){CategoryEntity e=get(id);if(!repo.findAllByParent_Id(id).isEmpty()||roles.existsByCategoryIdAndActiveTrue(id))throw new ConflictException("Deactivate child categories and roles before deactivating this category");e.setActive(false);}
}
