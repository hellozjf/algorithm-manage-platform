package com.zrar.ai.bo;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @author hellozjf
 *
 * 详见
 * https://blog.csdn.net/tianyaleixiaowu/article/details/77931903
 * https://www.jianshu.com/p/14cb69646195
 *
 * UUID生成器：
 * https://blog.csdn.net/vary_/article/details/8557043
 *
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseBO implements Serializable {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generator = "idGenerator")
    @GenericGenerator(name="idGenerator", strategy="uuid")
    private String id;

    /**
     * 创建时间
     */
    @CreatedDate
    @Temporal(TemporalType.DATE)
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Temporal(TemporalType.DATE)
    private Date gmtModified;
}