package com.zrar.ai.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Jingfeng Zhou
 *
 * 参考：http://www.zuidaima.com/question/2049879483862016.htm
 * 这里的@NoRepositoryBean告诉spring容器不要把BaseDao注入为一个bean
 */
@NoRepositoryBean
public interface BaseDao<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor {
}
