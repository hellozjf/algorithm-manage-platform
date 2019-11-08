package com.zrar.ai.service.impl;

import com.zrar.ai.bo.DictBO;
import com.zrar.ai.dao.DictDao;
import com.zrar.ai.mapper.DictMapper;
import com.zrar.ai.service.DictService;
import com.zrar.ai.vo.DictVO;
import org.springframework.stereotype.Service;

/**
 * @author Jingfeng Zhou
 */
@Service
public class DictServiceImpl extends BaseServiceImpl<DictVO, DictBO, DictDao, DictMapper>
        implements DictService {
}
