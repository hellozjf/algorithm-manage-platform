package com.zrar.ai.repository;

import com.zrar.ai.domain.DictItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Jingfeng Zhou
 */
public interface DictItemRepository extends JpaRepository<DictItemEntity, String> {
}
