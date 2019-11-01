package com.zrar.algorithm.vo;

import com.zrar.algorithm.constant.ModelTypeEnum;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

/**
 * 容器名称VO对象
 *
 * @author Jingfeng Zhou
 */
@Data
public class FullNameVO {

    @Value("${custom.docker.prefix}")
    private static String dockerPrefix;

    /**
     * 完成的名称，包括 前缀-类型-名称-版本
     */
    private String fullName;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 字符串类型的类型
     */
    private String strType;

    /**
     * 数字类型的类型
     */
    private int iType;

    /**
     * 名称
     */
    private String name;

    /**
     * 版本
     */
    private int version;

    /**
     * 通过前缀-类型-名称-版本号，获取FullName
     * @param fullName
     * @return
     */
    public static FullNameVO getByFullName(String fullName) {
        FullNameVO fullNameVO = new FullNameVO();
        String[] parts = fullName.split("-");
        fullNameVO.fullName = fullName;
        fullNameVO.prefix = parts[0];
        fullNameVO.strType = parts[1];
        fullNameVO.iType = ModelTypeEnum.getCodeByDesc(fullNameVO.strType);
        fullNameVO.name = String.join("-", Arrays.copyOfRange(parts, 2, parts.length - 1));
        fullNameVO.version = Integer.valueOf(parts[parts.length - 1]);
        return fullNameVO;
    }

    /**
     * 通过类型、名称、版本号获取FullName
     * @param type
     * @param name
     * @param version
     * @return
     */
    public static FullNameVO getByTypeNameVersion(int type, String name, int version) {
        String fullName = String.join("-", dockerPrefix, ModelTypeEnum.getDescByCode(type), name, String.valueOf(version));
        return getByFullName(fullName);
    }
}
