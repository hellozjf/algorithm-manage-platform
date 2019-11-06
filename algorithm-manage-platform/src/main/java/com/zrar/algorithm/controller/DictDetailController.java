package com.zrar.algorithm.controller;

import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.domain.DictDetailEntity;
import com.zrar.algorithm.domain.DictEntity;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.repository.DictDetailRepository;
import com.zrar.algorithm.repository.DictRepository;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 字典详情
 * @author Jingfeng Zhou
 */
@Slf4j
@RestController
@RequestMapping("/dictDetail")
public class DictDetailController {

    @Autowired
    private DictDetailRepository dictDetailRepository;

    @Autowired
    private DictRepository dictRepository;

    /**
     * 查询
     * @param pageable
     * @return
     */
    @GetMapping
    public ResultVO getAllDictDetails(Pageable pageable) {
        Page<DictDetailEntity> dictDetailEntityPage = dictDetailRepository.findAll(pageable);
        return ResultUtils.success(dictDetailEntityPage);
    }

    /**
     * 新增
     * @param dictDetailEntity
     * @return
     */
    @PostMapping
    public ResultVO addDictDetail(@RequestBody DictDetailEntity dictDetailEntity) {
        dictDetailEntity.setId(null);
        dictDetailEntity = dictDetailRepository.save(dictDetailEntity);
        return ResultUtils.success(dictDetailEntity);
    }

    /**
     * 修改
     * @param dictDetailEntity
     * @return
     */
    @PutMapping
    public ResultVO updateDictDetail(@RequestBody DictDetailEntity dictDetailEntity) {
        DictDetailEntity oldDictDetailEntity = dictDetailRepository.findById(dictDetailEntity.getId()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_ERROR));
        oldDictDetailEntity.setLabel(dictDetailEntity.getLabel());
        oldDictDetailEntity.setSort(dictDetailEntity.getSort());
        oldDictDetailEntity.setValue(dictDetailEntity.getValue());
        DictEntity dictEntity = new DictEntity();
        dictEntity.setId(dictDetailEntity.getDict().getId());
        oldDictDetailEntity.setDict(dictEntity);
        oldDictDetailEntity = dictDetailRepository.save(oldDictDetailEntity);
        return ResultUtils.success(oldDictDetailEntity);
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    public ResultVO deleteDictDetails(@RequestParam String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            dictDetailRepository.deleteById(id);
        }
        return ResultUtils.success();
    }
}
