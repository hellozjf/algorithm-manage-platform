package com.zrar.ai.service;

import com.zrar.ai.vo.BaseQueryVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Jingfeng Zhou
 */
public interface BaseService<V> {

    /**
     * 根据分页查询
     * @param pageable
     * @return
     */
    Page<V> getPage(BaseQueryVO queryVO, Pageable pageable);

    /**
     * 添加
     * @param vo
     * @return
     */
    V insert(V vo);

    /**
     * 修改
     * @param vo
     * @return
     */
    V update(V vo);

    /**
     * 删除
     * @param vo
     */
    void delete(V vo);

    /**
     * 根据ID删除
     * @param id
     */
    void deleteById(String id);
}
