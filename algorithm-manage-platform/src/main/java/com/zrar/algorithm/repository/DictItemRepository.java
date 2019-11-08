package com.zrar.algorithm.repository;

import com.zrar.algorithm.domain.DictItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Jingfeng Zhou
 */
public interface DictItemRepository extends JpaRepository<DictItemEntity, String> {
}
