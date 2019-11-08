package com.zrar.ai.service;

import com.zrar.ai.vo.DictItemVO;
import com.zrar.ai.vo.DictVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 数据字典项目接口
 * @author Jingfeng Zhou
 */
public interface DictItemService {

    /**
     * 根据分页查询数据字典项目
     * @param pageable
     * @return
     */
    Page<DictItemVO> getDictPage(Pageable pageable);

    /**
     * 添加数据字典项目
     * @param vo
     * @return
     */
    DictItemVO insertDict(DictItemVO vo);

    /**
     * 修改数据字典项目
     * @param vo
     * @return
     */
    DictItemVO updateDict(DictItemVO vo);

    /**
     * 删除数据字典项目
     * @param vo
     */
    void deleteDict(DictItemVO vo);
}
