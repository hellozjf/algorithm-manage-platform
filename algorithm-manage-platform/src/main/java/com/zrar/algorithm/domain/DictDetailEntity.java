package com.zrar.algorithm.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

/**
 * @author Zheng Jie
 * @date 2019-04-10
 */
@Slf4j
@Entity
@Data
@Table(name = "dict_detail")
public class DictDetailEntity extends BaseEntity {

    /**
     * 字典标签
     */
    @Column(name = "label", nullable = false)
    private String label;

    /**
     * 字典值
     */
    @Column(name = "value", nullable = false)
    private String value;

    /**
     * 排序
     */
    @Column(name = "sort")
    private String sort;

    /**
     * 字典id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dict_id")
    private DictEntity dict;
}