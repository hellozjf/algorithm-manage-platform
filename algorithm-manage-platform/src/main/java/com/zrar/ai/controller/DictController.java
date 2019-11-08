package com.zrar.ai.controller;

import com.zrar.ai.bo.DictBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.mapper.DictMapper;
import com.zrar.ai.dao.DictDao;
import com.zrar.ai.util.ResultUtils;
import com.zrar.ai.vo.DictVO;
import com.zrar.ai.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 字典列表
 * @author Jingfeng Zhou
 */
@Slf4j
@RestController
@RequestMapping("/dict")
public class DictController {

    @Autowired
    private DictDao dictRepository;

    @Autowired
    private DictMapper dictMapper;

    /**
     * 查询
     * @param pageable
     * @return
     */
    @GetMapping
    public ResultVO getAllDicts(Pageable pageable) {
        Page<DictBO> dictEntityPage = dictRepository.findAll(pageable);
        Page<DictVO> dictVOPage = dictEntityPage.map(dictMapper::toVO);
        return ResultUtils.success(dictVOPage);
    }

    /**
     * 新增
     * @param dictEntity
     * @return
     */
    @PostMapping
    public ResultVO addDict(@RequestBody DictBO dictEntity) {
        dictEntity.setId(null);
        dictEntity = dictRepository.save(dictEntity);
        DictVO dictVO = dictMapper.toVO(dictEntity);
        return ResultUtils.success(dictVO);
    }

    /**
     * 修改
     * @param dictEntity
     * @return
     */
    @PutMapping
    public ResultVO updateDict(@RequestBody DictBO dictEntity) {
        DictBO oldDictEntity = dictRepository.findById(dictEntity.getId()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_ERROR));
        dictEntity.setGmtCreate(oldDictEntity.getGmtCreate());
        dictEntity = dictRepository.save(dictEntity);
        DictVO dictVO = dictMapper.toVO(dictEntity);
        return ResultUtils.success(dictVO);
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    public ResultVO deleteDicts(@RequestParam String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            dictRepository.deleteById(id);
        }
        return ResultUtils.success();
    }
}
