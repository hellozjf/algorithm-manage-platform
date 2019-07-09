/**
 * 
 */
package com.zrar.algorithm.validator;

import com.zrar.algorithm.constant.ModelTypeEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author zhailiang
 *
 */
public class ModelTypeConstraintValidator implements ConstraintValidator<ModelTypeConstraint, Integer> {

	/**
	 * 只有当模型类型在ModelTypeEnum中有的时候，才是合法的
	 * @param value
	 * @param context
	 * @return
	 */
	@Override
	public boolean isValid(Integer value, ConstraintValidatorContext context) {
		for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
			if (value.intValue() == modelTypeEnum.getCode()) {
				return true;
			}
		}
		return false;
	}

}
