/**
 *
 */
package com.zrar.algorithm.validator;

import com.zrar.algorithm.domain.ModelEntity;
import com.zrar.algorithm.repository.ModelRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

/**
 * @author zhailiang
 */
public class ModelNameConstraintValidator implements ConstraintValidator<ModelNameConstraint, String> {

    @Autowired
    private ModelRepository modelRepository;

    /**
     * 只有当模型类型在ModelParamEnum中有的时候，才是合法的
     *
     * @param value
     * @param context
     * @return
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        Optional<ModelEntity> modelEntity = modelRepository.findByName(value);
        if (modelEntity.isPresent()) {
            return false;
        } else {
            return true;
        }
    }

}
