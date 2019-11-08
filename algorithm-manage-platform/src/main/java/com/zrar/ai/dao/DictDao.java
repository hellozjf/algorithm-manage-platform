package com.zrar.ai.dao;

import com.zrar.ai.bo.DictBO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Jingfeng Zhou
 */
public interface DictDao extends JpaRepository<DictBO, String> {
}
