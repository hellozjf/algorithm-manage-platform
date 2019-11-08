package com.zrar.algorithm.controller;

import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.domain.DictItemEntity;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.mapper.DictItemMapper;
import com.zrar.algorithm.repository.DictItemRepository;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.DictItemVO;
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
    private DictItemRepository dictItemRepository;

    @Autowired
    private DictItemMapper dictItemMapper;

    /**
     * 查询
     * @param pageable
     * @return
     */
    @GetMapping
    public ResultVO getAllDictItems(Pageable pageable) {
        Page<DictItemEntity> dictItemEntityPage = dictItemRepository.findAll(pageable);
        Page<DictItemVO> dictItemVOPage = dictItemEntityPage.map(dictItemEntity -> {
            DictItemVO dictItemVO = dictItemMapper.toDto(dictItemEntity);
            dictItemVO.setDictId(dictItemEntity.getDict().getId());
            return dictItemVO;
        });
        return ResultUtils.success(dictItemVOPage);
    }

    /**
     * 新增
     * @param dictItemEntity
     * @return
     */
    @PostMapping
    public ResultVO addDictItem(@RequestBody DictItemEntity dictItemEntity) {
        dictItemEntity.setId(null);
        dictItemEntity = dictItemRepository.save(dictItemEntity);
        DictItemVO dictDetailVO = dictItemMapper.toDto(dictItemEntity);
        dictDetailVO.setDictId(dictItemEntity.getDict().getId());
        return ResultUtils.success(dictDetailVO);
    }

    /**
     * 修改
     * @param dictDetailEntity
     * @return
     */
    @PutMapping
    public ResultVO updateDictItem(@RequestBody DictItemEntity dictDetailEntity) {
        DictItemEntity oldDictDetailEntity = dictItemRepository.findById(dictDetailEntity.getId()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_DICT_ERROR));
        dictDetailEntity.setGmtCreate(oldDictDetailEntity.getGmtCreate());
        dictDetailEntity = dictItemRepository.save(dictDetailEntity);
        DictItemVO dictDetailVO = dictItemMapper.toDto(dictDetailEntity);
        dictDetailVO.setDictId(dictDetailEntity.getDict().getId());
        return ResultUtils.success(dictDetailVO);
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    public ResultVO deleteDictItems(@RequestParam String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            dictItemRepository.deleteById(id);
        }
        return ResultUtils.success();
    }
}
