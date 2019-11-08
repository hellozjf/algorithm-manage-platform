package com.zrar.ai.service;

import com.zrar.ai.vo.DictVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 数据字典接口
 * @author Jingfeng Zhou
 */
public interface DictService {

    /**
     * 根据分页查询数据字典
     * @param pageable
     * @return
     */
    Page<DictVO> getDictPage(Pageable pageable);

    /**
     * 添加数据字典
     * @param vo
     * @return
     */
    DictVO insertDict(DictVO vo);

    /**
     * 修改数据字典
     * @param vo
     * @return
     */
    DictVO updateDict(DictVO vo);

    /**
     * 删除数据字典
     * @param vo
     */
    void deleteDict(DictVO vo);
}
