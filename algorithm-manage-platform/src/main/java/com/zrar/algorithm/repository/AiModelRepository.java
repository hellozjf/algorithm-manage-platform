package com.zrar.algorithm.repository;

import com.zrar.algorithm.domain.AiModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Jingfeng Zhou
 */
public interface AiModelRepository extends JpaRepository<AiModelEntity, String> {

    /**
     * 通过类型、名称、版本号获取数据库保存的实体
     * @param type
     * @param shortName
     * @param version
     * @return
     */
    Optional<AiModelEntity> findByTypeAndShortNameAndVersion(int type, String shortName, int version);

    /**
     * 找出所有模型名称叫做name的实体
     * @param shortName
     * @return
     */
    List<AiModelEntity> findByShortName(String shortName);
}
