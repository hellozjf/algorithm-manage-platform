package com.zrar.ai.service.impl;

import com.zrar.ai.bo.BaseBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.BaseDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.mapper.BaseMapper;
import com.zrar.ai.service.BaseService;
import com.zrar.ai.util.QueryHelp;
import com.zrar.ai.vo.BaseQueryVO;
import com.zrar.ai.vo.BaseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jingfeng Zhou
 */
public abstract class BaseServiceImpl<
        V extends BaseVO,
        B extends BaseBO,
        D extends BaseDao<B, String>,
        M extends BaseMapper<V, B>
        > implements BaseService<V> {

    @Autowired
    protected D dao;

    @Autowired
    protected M mapper;

    @Override
    public Page<V> getPage(BaseQueryVO queryVO, Pageable pageable) {
        Page<B> boPage = dao.findAll((root, query, cb) -> QueryHelp.getPredicate(root, queryVO, cb), pageable);
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
        dao.deleteById(vo.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(String id) {
        dao.deleteById(id);
    }
}
