package com.zrar.ai.mapper;

import com.zrar.ai.bo.DictBO;
import com.zrar.ai.vo.DictVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * VO与BO互相转换工具类
 * @author hellozjf
 * @date 2019-11-08
 */
@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictMapper extends BaseMapper<DictVO, DictBO> {

}