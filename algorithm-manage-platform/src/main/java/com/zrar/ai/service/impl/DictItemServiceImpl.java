package com.zrar.ai.service.impl;

import com.zrar.ai.bo.DictItemBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.DictItemDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.mapper.DictItemMapper;
import com.zrar.ai.service.DictItemService;
import com.zrar.ai.vo.DictItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @author Jingfeng Zhou
 */
@Service
public class DictItemServiceImpl implements DictItemService {

    @Autowired
    private DictItemDao dao;

    @Autowired
    private DictItemMapper mapper;

    @Override
    public Page<DictItemVO> getDictPage(Pageable pageable) {
        Page<DictItemBO> dictItemBOPage = dao.findAll(pageable);
        Page<DictItemVO> dictItemVOPage = dictItemBOPage.map(mapper::toVO);
        return dictItemVOPage;
    }

    @Override
    public DictItemVO insertDict(DictItemVO vo) {
        DictItemBO bo = mapper.toBO(vo);
        bo.setId(null);
        bo = dao.save(bo);
        DictItemVO newVO = mapper.toVO(bo);
        return newVO;
    }

    @Override
    public DictItemVO updateDict(DictItemVO vo) {
        DictItemBO bo = mapper.toBO(vo);
        DictItemBO oldBO = dao.findById(bo.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_DETAIL_ERROR));
        bo.setGmtCreate(oldBO.getGmtCreate());
        bo = dao.save(bo);
        DictItemVO newVO = mapper.toVO(bo);
        return newVO;
    }

    @Override
    public void deleteDict(DictItemVO vo) {
        DictItemBO bo = dao.findById(vo.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_DETAIL_ERROR));
        dao.delete(bo);
    }
}
