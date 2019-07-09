package com.zrar.algorithm.repository;

import com.zrar.algorithm.domain.ModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Jingfeng Zhou
 */
public interface ModelRepository extends JpaRepository<ModelEntity, String> {
    ModelEntity findByName(String name);
}
