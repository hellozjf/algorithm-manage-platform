package com.zrar.ai.controller;

import com.zrar.ai.service.DictService;
import com.zrar.ai.vo.DictQueryVO;
import com.zrar.ai.vo.DictVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 字典列表
 *
 * @author Jingfeng Zhou
 */
@Slf4j
@RestController
@Api(tags = "数据字典管理接口")
@RequestMapping("/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    /**
     * 查询数据字典分页
     *
     * @param pageable
     * @return
     */
    @ApiOperation("通过条件和分页查询数据字典")
    @GetMapping
    public ResponseEntity getAllDicts(DictQueryVO queryVO, Pageable pageable) {
        Page<DictVO> dictVOPage = dictService.getPage(queryVO, pageable);
        return new ResponseEntity(dictVOPage, HttpStatus.OK);
    }

    /**
     * 新增数据字典
     *
     * @param dictVO
     * @return
     */
    @ApiOperation("添加数据字典")
    @PostMapping
    public ResponseEntity addDict(@RequestBody DictVO dictVO) {
        DictVO newDictVO = dictService.insert(dictVO);
        return new ResponseEntity(newDictVO, HttpStatus.CREATED);
    }

    /**
     * 修改数据字典
     *
     * @param dictVO
     * @return
     */
    @ApiOperation("更新数据字典")
    @PutMapping
    public ResponseEntity updateDict(@RequestBody DictVO dictVO) {
        dictService.update(dictVO);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 删除数据字典
     *
     * @param id
     * @return
     */
    @ApiOperation("删除数据字典")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity deleteDicts(@PathVariable String id) {
        dictService.deleteById(id);
        return new ResponseEntity(HttpStatus.OK);
    }
}
