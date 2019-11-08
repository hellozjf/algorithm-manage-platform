package com.zrar.ai.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

/**
 * 数据字典项目
 * @author hellozjf
 * @date 2019-11-08
 */
@Slf4j
@Entity
@Data
@Table(name = "dict_detail")
public class DictItemEntity extends BaseEntity {

    /**
     * 值
     */
    private String value;

    /**
     * 显示文本
     */
    private String text;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 其它值
     */
    private String other;

    /**
     * 字典id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dict_id")
    private DictEntity dict;
}