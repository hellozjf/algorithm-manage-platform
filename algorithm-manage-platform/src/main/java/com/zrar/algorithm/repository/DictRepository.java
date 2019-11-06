package com.zrar.algorithm.repository;

import com.zrar.algorithm.domain.DictEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Jingfeng Zhou
 */
public interface DictRepository extends JpaRepository<DictEntity, String> {
}
