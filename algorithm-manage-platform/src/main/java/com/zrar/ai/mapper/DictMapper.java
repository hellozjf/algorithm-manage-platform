package com.zrar.ai.mapper;

import com.zrar.ai.domain.DictEntity;
import com.zrar.ai.vo.DictVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @author Zheng Jie
 * @date 2019-04-10
 */
@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictMapper extends BaseMapper<DictVO, DictEntity> {

}