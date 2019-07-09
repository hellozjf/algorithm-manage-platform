/**
 *
 */
package com.zrar.algorithm.validator;

import com.zrar.algorithm.constant.ModelParamEnum;
import com.zrar.algorithm.constant.ModelTypeEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author zhailiang
 */
public class ModelParamConstraintValidator implements ConstraintValidator<ModelParamConstraint, Integer> {

    /**
     * 只有当模型类型在ModelParamEnum中有的时候，才是合法的
     *
     * @param value
     * @param context
     * @return
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        for (ModelParamEnum modelParamEnum : ModelParamEnum.values()) {
            if (value.intValue() == modelParamEnum.getCode()) {
                return true;
            }
        }
        return false;
    }

}
