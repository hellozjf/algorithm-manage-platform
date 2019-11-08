package com.zrar.ai.controller;

import com.zrar.ai.service.DictItemService;
import com.zrar.ai.vo.DictItemQueryVO;
import com.zrar.ai.vo.DictItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 字典详情
 * @author Jingfeng Zhou
 */
@Slf4j
@RestController
@RequestMapping("/dictItem")
public class DictItemController {

    @Autowired
    private DictItemService dictItemService;

    /**
     * 查询
     * @param pageable
     * @return
     */
    @GetMapping
    public ResponseEntity getAllDictItems(DictItemQueryVO queryVO, Pageable pageable) {
        Page<DictItemVO> dictItemVOPage = dictItemService.getPage(queryVO, pageable);
        return new ResponseEntity(dictItemVOPage, HttpStatus.OK);
    }

    /**
     * 新增
     * @param dictItemVO
     * @return
     */
    @PostMapping
    public ResponseEntity addDictItem(@RequestBody DictItemVO dictItemVO) {
        DictItemVO newDictItemVO = dictItemService.insert(dictItemVO);
        return new ResponseEntity(newDictItemVO, HttpStatus.CREATED);
    }

    /**
     * 修改
     * @param dictItemVO
     * @return
     */
    @PutMapping
    public ResponseEntity updateDictItem(@RequestBody DictItemVO dictItemVO) {
        dictItemService.update(dictItemVO);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}")
    public ResponseEntity deleteDictItems(@PathVariable String id) {
        dictItemService.deleteById(id);
        return new ResponseEntity(HttpStatus.OK);
    }
}
