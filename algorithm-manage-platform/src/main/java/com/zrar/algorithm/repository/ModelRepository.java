package com.zrar.algorithm.repository;

import com.zrar.algorithm.domain.ModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Jingfeng Zhou
 */
public interface ModelRepository extends JpaRepository<ModelEntity, String> {
    Optional<ModelEntity> findByName(String name);
}
