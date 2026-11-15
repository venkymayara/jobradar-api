package com.jobradar.taxonomy.category;
import com.jobradar.taxonomy.common.TaxonomyEntity;
import jakarta.persistence.*;
@Entity @Table(name="categories")
public class CategoryEntity extends TaxonomyEntity {
 @Column(nullable=false,length=120) private String name;
 @Column(nullable=false,length=140) private String slug;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_category_id") private CategoryEntity parent;
 @Column(length=500) private String description;
 @Column(length=80) private String icon;
 protected CategoryEntity() {}
 public CategoryEntity(String name,String slug,CategoryEntity parent,String description,String icon){this.name=name;this.slug=slug;this.parent=parent;this.description=description;this.icon=icon;}
 public String getName(){return name;} public String getSlug(){return slug;} public CategoryEntity getParent(){return parent;} public String getDescription(){return description;} public String getIcon(){return icon;}
 public void update(String n,String s,CategoryEntity p,String d,String i){name=n;slug=s;parent=p;description=d;icon=i;}
}
