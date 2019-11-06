package com.zrar.algorithm.domain;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

/**
 * @author Zheng Jie
 * @date 2019-04-10
 */
@Entity
@Data
@Table(name = "dict")
public class DictEntity extends BaseEntity {

    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典描述
     */
    private String remark;

    @OneToMany(mappedBy = "dict", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<DictDetailEntity> dictDetails;
}