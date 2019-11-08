package com.zrar.ai.service.impl;

import com.zrar.ai.bo.BaseBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.mapper.BaseMapper;
import com.zrar.ai.service.BaseService;
import com.zrar.ai.vo.BaseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jingfeng Zhou
 */
public abstract class BaseServiceImpl<
        V extends BaseVO,
        B extends BaseBO,
        D extends JpaRepository<B, String>,
        M extends BaseMapper<V, B>
        > implements BaseService<V> {

    @Autowired
    private D dao;

    @Autowired
    private M mapper;

    @Override
    public Page<V> getPage(Pageable pageable) {
        Page<B> boPage = dao.findAll(pageable);
        Page<V> voPage = boPage.map(mapper::toVO);
        return voPage;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public V insert(V vo) {
        B bo = mapper.toBO(vo);
        bo.setId(null);
        bo = dao.save(bo);
        V newVO = mapper.toVO(bo);
        return newVO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public V update(V vo) {
        B bo = mapper.toBO(vo);
        B oldBO = dao.findById(bo.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_ERROR));
        bo.setGmtCreate(oldBO.getGmtCreate());
        bo = dao.save(bo);
        V newVO = mapper.toVO(bo);
        return newVO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(V vo) {
        B bo = dao.findById(vo.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_ERROR));
        dao.delete(bo);
    }
}
