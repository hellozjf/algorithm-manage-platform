package com.zrar.ai.controller;

import com.zrar.ai.service.DictItemService;
import com.zrar.ai.vo.DictItemQueryVO;
import com.zrar.ai.vo.DictItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "数据字典项目管理接口")
@RequestMapping("/dictItem")
public class DictItemController {

    @Autowired
    private DictItemService dictItemService;

    /**
     * 查询
     * @param pageable
     * @return
     */
    @ApiOperation("通过条件和分页查询数据字典项目")
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
    @ApiOperation("添加数据字典项目")
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
    @ApiOperation("更新数据字典项目")
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
    @ApiOperation("删除数据字典项目")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity deleteDictItems(@PathVariable String id) {
        dictItemService.deleteById(id);
        return new ResponseEntity(HttpStatus.OK);
    }
}
