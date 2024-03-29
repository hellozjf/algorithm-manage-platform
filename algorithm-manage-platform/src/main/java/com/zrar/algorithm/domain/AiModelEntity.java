package com.zrar.algorithm.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * 数据库中，模型的实体类
 *
 * @author Jingfeng Zhou
 */
@Slf4j
@Data
@Entity
public class AiModelEntity extends BaseEntity {

    /**
     * 模型的名称，例如sw、yyth、qgfx……
     * 实际的模型文件后面需要加.zip后缀
     * 模型的名称也是后续algorithm-bridge需要的模型的路径
     */
    @Column(unique = true)
    private String name;

    /**
     * 模型描述
     */
    private String desc;

    /**
     * 模型md5
     */
    private String md5;

    /**
     * 模型类型，目前支持mleap和tensorflow
     * @see com.zrar.algorithm.constant.ModelTypeEnum
     */
    private int type;

    /**
     * 模型参数
     * 如果模型类型是mleap，则支持切词、切词——税务专有词、切短语
     * 如果模型类型是tensorflow，则支持脏话、情感分析
     *
     * 下面的JSON内容依次是：
     * 移除标点
     * 去停词
     * 长度
     * 参数代码（为了兼容之前int类型的param）
     * 组合（当模型类型是ModelTypeEnum.COMPOSE时，依次将前一个模型的结果送给下一个模型）
     *
     * {
     *  "removePunctuation": false,
     *  "removeStopWord": true,
     *  "length": 128,
     *  "paramCode": 102,
     *  "compose": "ap,bi"
     * }
     *
     * @see com.zrar.algorithm.constant.ModelParamEnum
     */
    private String param;

    /**
     * 版本号，从1开始，每次修改增加1
     */
    private int version;
}
