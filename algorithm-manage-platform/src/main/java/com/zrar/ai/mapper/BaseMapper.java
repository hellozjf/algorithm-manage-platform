package com.zrar.ai.mapper;

import java.util.List;

/**
 * VO与BO互相转换工具类
 * @author hellozjf
 * @date 2019-11-08
 */
public interface BaseMapper<V, B> {

    /**
     * VO转BO
     * @param vo
     * @return
     */
    B toBO(V vo);

    /**
     * BO转VO
     * @param bo
     * @return
     */
    V toVO(B bo);

    /**
     * VO集合转BO集合
     * @param voList
     * @return
     */
    List<B> toBO(List<V> voList);

    /**
     * BO集合转VO集合
     * @param boList
     * @return
     */
    List <V> toVO(List<B> boList);
}
