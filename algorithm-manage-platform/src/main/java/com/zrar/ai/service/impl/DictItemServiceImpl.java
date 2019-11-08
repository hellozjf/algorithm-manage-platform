package com.zrar.ai.service.impl;

import com.zrar.ai.bo.DictBO;
import com.zrar.ai.bo.DictItemBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.DictItemDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.mapper.DictItemMapper;
import com.zrar.ai.service.DictItemService;
import com.zrar.ai.vo.DictItemVO;
import org.springframework.stereotype.Service;

/**
 * @author Jingfeng Zhou
 */
@Service
public class DictItemServiceImpl extends BaseServiceImpl<DictItemVO, DictItemBO, DictItemDao, DictItemMapper>
        implements DictItemService {

    @Override
    public DictItemVO insert(DictItemVO vo) {
        DictItemBO dictItemBO = mapper.toBO(vo);
        dictItemBO.setId(null);
        DictBO dictBO = new DictBO();
        dictBO.setId(vo.getDictId());
        dictItemBO.setDict(dictBO);
        dictItemBO = dao.save(dictItemBO);
        DictItemVO newDictItemVO = mapper.toVO(dictItemBO);
        return newDictItemVO;
    }

    @Override
    public DictItemVO update(DictItemVO vo) {
        DictItemBO dictItemBO = mapper.toBO(vo);
        DictItemBO oldDictItemBO = dao.findById(vo.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_DETAIL_ERROR));
        dictItemBO.setGmtCreate(oldDictItemBO.getGmtCreate());
        DictBO dictBO = new DictBO();
        dictBO.setId(vo.getDictId());
        dictItemBO.setDict(dictBO);
        dictItemBO = dao.save(dictItemBO);
        DictItemVO newDictItemVO = mapper.toVO(dictItemBO);
        return newDictItemVO;
    }
}
