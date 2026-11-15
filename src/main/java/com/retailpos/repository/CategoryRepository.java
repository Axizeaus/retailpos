
package com.retailpos.repository;

import com.retailpos.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByShopIdAndActiveTrue(Long shopId);

    Optional<Category> findByIdAndShopId(Long id, Long shopId);

    boolean existsByShopIdAndName(Long shopId, String name);

    boolean existsByShopIdAndNameAndIdNot(
            Long shopId,
            String name,
            Long id
    );
}
