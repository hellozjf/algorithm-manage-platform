package com.zrar.algorithm.form;

import com.zrar.algorithm.validator.ModelNameConstraint;
import com.zrar.algorithm.validator.ModelParamConstraint;
import com.zrar.algorithm.validator.ModelTypeConstraint;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotBlank;

/**
 * 数据库中，模型的实体类
 *
 * @author Jingfeng Zhou
 */
@Slf4j
@Data
public class ModelForm {

    /**
     * 模型的编号
     */
    private String id;

    /**
     * 模型的名称，例如sw、yyth、qgfx……
     * 实际的模型文件后面需要加.zip后缀
     * 模型的名称也是后续algorithm-bridge需要的模型的路径
     */
    @NotBlank(message = "模型名称不能为空")
    @ModelNameConstraint(message = "模型名已存在")
    private String name;

    /**
     * 模型描述
     */
    private String desc;

    /**
     * 模型类型，目前支持mleap和tensorflow
     * @see com.zrar.algorithm.constant.ModelTypeEnum
     */
    @ModelTypeConstraint(message = "模型类型不合法")
    private Integer type;

    /**
     * 模型参数
     * 如果模型类型是mleap，则支持切词、切词——税务专有词、切短语
     * 如果模型类型是tensorflow，则支持脏话
     * @see com.zrar.algorithm.constant.ModelParamEnum
     */
    @ModelParamConstraint(message = "模型参数不合法")
    private Integer param;

    /**
     * 模型组合
     */
    private String compose;
}
