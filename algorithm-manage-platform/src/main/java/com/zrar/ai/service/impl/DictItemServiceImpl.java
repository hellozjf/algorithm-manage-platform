package com.zrar.ai.service.impl;

import com.zrar.ai.bo.DictItemBO;
import com.zrar.ai.dao.DictItemDao;
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
}
