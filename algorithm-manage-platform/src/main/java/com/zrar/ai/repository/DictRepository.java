package com.zrar.ai.repository;

import com.zrar.ai.domain.DictEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Jingfeng Zhou
 */
public interface DictRepository extends JpaRepository<DictEntity, String> {
}
