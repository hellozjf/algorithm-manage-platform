package com.zrar.algorithm.mapper;

import com.zrar.algorithm.domain.DictDetailEntity;
import com.zrar.algorithm.vo.DictDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @author Zheng Jie
 * @date 2019-04-10
 */
@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictDetailMapper extends BaseMapper<DictDetailVO, DictDetailEntity> {

}