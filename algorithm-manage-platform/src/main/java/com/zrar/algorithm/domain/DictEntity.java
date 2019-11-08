package com.zrar.algorithm.domain;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

/**
 * 数据字典
 * @author hellozjf
 * @date 2019-11-08
 */
@Entity
@Data
@Table(name = "dict")
public class DictEntity extends BaseEntity {

    /**
     * 字典编码
     */
    private String code;

    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典描述
     */
    private String description;

    @OneToMany(mappedBy = "dict", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<DictItemEntity> dictDetails;
}