package com.zrar.ai.service.impl;

import com.zrar.ai.bo.DictBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.mapper.DictMapper;
import com.zrar.ai.dao.DictDao;
import com.zrar.ai.service.DictService;
import com.zrar.ai.vo.DictVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jingfeng Zhou
 */
@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private DictDao dao;

    @Autowired
    private DictMapper mapper;

    @Override
    public Page<DictVO> getDictPage(Pageable pageable) {
        Page<DictBO> boPage = dao.findAll(pageable);
        Page<DictVO> voPage = boPage.map(mapper::toVO);
        return voPage;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DictVO insertDict(DictVO vo) {
        DictBO bo = mapper.toBO(vo);
        bo.setId(null);
        bo = dao.save(bo);
        DictVO newVO = mapper.toVO(bo);
        return newVO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DictVO updateDict(DictVO vo) {
        DictBO bo = mapper.toBO(vo);
        DictBO oldBO = dao.findById(bo.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_ERROR));
        bo.setGmtCreate(oldBO.getGmtCreate());
        bo = dao.save(bo);
        DictVO newVO = mapper.toVO(bo);
        return newVO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteDict(DictVO vo) {
        DictBO bo = dao.findById(vo.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_ERROR));
        dao.delete(bo);
    }
}
