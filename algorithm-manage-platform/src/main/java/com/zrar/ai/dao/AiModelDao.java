package com.zrar.ai.dao;

import com.zrar.ai.bo.AiModelBO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Jingfeng Zhou
 */
public interface AiModelDao extends JpaRepository<AiModelBO, String> {

    /**
     * 通过类型、名称、版本号获取数据库保存的实体
     * @param type
     * @param shortName
     * @param version
     * @return
     */
    Optional<AiModelBO> findByTypeAndShortNameAndVersion(int type, String shortName, int version);

    /**
     * 通过类型、名称，获取版本号最大的一条实体记录
     * @param type
     * @param shortName
     * @return
     */
    Optional<AiModelBO> findTopByTypeAndShortNameOrderByVersionDesc(int type, String shortName);

    /**
     * 找出所有模型名称叫做name的实体
     * @param shortName
     * @return
     */
    List<AiModelBO> findByShortName(String shortName);

    /**
     * 根据端口寻找实体
     * @param port
     * @return
     */
    List<AiModelBO> findByPort(int port);
}
