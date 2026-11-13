package com.retailpos.service;

import com.retailpos.dto.CategoryCreateRequest;
import com.retailpos.dto.CategoryResponse;
import com.retailpos.dto.CategoryUpdateRequest;
import com.retailpos.entity.Category;
import com.retailpos.exception.CategoryInUseException;
import com.retailpos.exception.CategoryNotFoundException;
import com.retailpos.exception.DuplicateCategoryException;
import com.retailpos.repository.CategoryRepository;
import com.retailpos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request){
        if (categoryRepository.existsByName(request.name())){
            throw new DuplicateCategoryException(request.name());
        }

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());

        Category savedCategory = categoryRepository.save(category);

        return toCategoryResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories(){
        return categoryRepository.findAllByActiveTrue().stream().map(this::toCategoryResponse).toList();
    }

    public CategoryResponse getCategoryById(Long id){
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));

        return toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (!category.getName().equals(request.name())
                && categoryRepository.existsByName(request.name())) {
            throw new DuplicateCategoryException(request.name());
        }

        category.setName(request.name());
        category.setDescription(request.description());
        category.setActive(true);

        return toCategoryResponse(category);
    }

    @Transactional
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (productRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new CategoryInUseException(id);
        }

        category.setActive(false);
    }

    private CategoryResponse toCategoryResponse(Category category){
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }

}