package com.zrar.algorithm.controller;

import com.zrar.algorithm.config.CustomConfig;
import com.zrar.algorithm.constant.ModelParamEnum;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.domain.ModelEntity;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.form.ModelForm;
import com.zrar.algorithm.repository.ModelRepository;
import com.zrar.algorithm.service.DockerService;
import com.zrar.algorithm.service.FileService;
import com.zrar.algorithm.service.MLeapService;
import com.zrar.algorithm.service.RemoteService;
import com.zrar.algorithm.util.ModelParamUtils;
import com.zrar.algorithm.util.ModelTypeUtils;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.ModelVO;
import com.zrar.algorithm.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 页面上所使用的controller
 *
 * @author Jingfeng Zhou
 */
@Slf4j
@RestController
@RequestMapping("/web")
public class WebController {

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private FileService fileService;

    @Value("${spring.profiles.active}")
    private String active;

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private Runtime runtime;

    @Autowired
    private MLeapService mLeapService;

    @Autowired
    private RemoteService remoteService;

    @Autowired
    private DockerService dockerService;

    /**
     * 获取所有的模型
     *
     * @return
     */
    @GetMapping("/getAllModels")
    public ResultVO getAllModels(Pageable pageable) {
        Page<ModelEntity> modelEntityPage = modelRepository.findAll(pageable);
        List<ModelEntity> modelEntityList = modelEntityPage.getContent();
        List<ModelVO> modelVOList = getModelVOList(modelEntityList);
        Page<ModelVO> modelVOPage = new PageImpl<>(modelVOList, modelEntityPage.getPageable(), modelEntityPage.getTotalElements());
        return ResultUtils.success(modelVOPage);
    }

    private List<ModelVO> getModelVOList(List<ModelEntity> modelEntityList) {
        return modelEntityList.stream().map(modelEntity -> {
            ModelVO modelVO = new ModelVO();
            BeanUtils.copyProperties(modelEntity, modelVO);
            modelVO.setTypeName(ModelTypeUtils.getDescByCode(modelVO.getType()));
            modelVO.setParamName(ModelParamUtils.getDescByCode(modelVO.getParam()));
            return modelVO;
        }).collect(Collectors.toList());
    }

    /**
     * 获取easyui所需要的模型列表
     * @return
     */
    @GetMapping("/getAllModelList")
    public Map<String, Object> getAllModelList() {
        List<ModelEntity> modelEntityList = modelRepository.findAll();
        List<ModelVO> modelVOList = getModelVOList(modelEntityList);
        Map<String, Object> map = new HashMap<>();
        map.put("total", modelVOList.size());
        map.put("rows", modelVOList);
        return map;
    }

    /**
     * 添加模型
     *
     * @param modelForm
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/addModel")
    public ResultVO addModel(@RequestParam("file") MultipartFile multipartFile,
                             @Valid ModelForm modelForm,
                             BindingResult bindingResult) {

        // 判断上传过来的文件是不是空的
        if (multipartFile.isEmpty()) {
            return ResultUtils.error(ResultEnum.FILE_CAN_NOT_BE_EMPTY);
        }

        // 获取文件名
        File file = new File(fileService.getModelOutterPath(modelForm.getName()));

        // 将上传上来的文件保存到 customConfig.modelOuterPath 目录下
        try (InputStream inputStream = multipartFile.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            log.error("e = {}", e);
            if (file.exists()) {
                file.delete();
            }
            return ResultUtils.error(ResultEnum.FILE_IS_WRONG.getCode(), e.getMessage());
        }

        // dev版本，还需要把model文件拷贝到服务器上面去
        if (active.equalsIgnoreCase("dev")) {
            try {
                String cmd = remoteService.createScpCommand(fileService.getModelOutterPath(modelForm.getName()),
                        "/opt/docker/algorithm-manage-platform/models");
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.CMD_ERROR);
            }
        }

        // 如果是tensorflow模型，还需要将模型zip进行解压
        if (modelForm.getType() == ModelTypeEnum.TENSORFLOW.getCode()) {
            try {
                // 进入宿主机模型目录，unzip模型
                if (active.equalsIgnoreCase("dev")) {
                    String cmd = "unzip -o " + customConfig.getModelOuterPath() + "/" + modelForm.getName() + ".zip -d " + customConfig.getModelOuterPath() + "/" + modelForm.getName();
                    Process process = runtime.exec(cmd);
                    log.debug("{} return {}", cmd, process.waitFor());
                } else if (active.equalsIgnoreCase("prod")) {
                    String cmd = "cd " + customConfig.getModelOuterPath() + "; unzip -o " + modelForm.getName() + ".zip -d " + modelForm.getName();
                    Process process = runtime.exec(cmd);
                    log.debug("{} return {}", cmd, process.waitFor());
                }
                if (active.equalsIgnoreCase("dev")) {
                    // 开发环境，还要进入远程服务器unzip
                    String cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform/models/; unzip -o " + modelForm.getName() + ".zip -d " + modelForm.getName());
                    Process process = runtime.exec(cmd);
                    log.debug("{} return {}", cmd, process.waitFor());
                }
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.CMD_ERROR);
            }
        }

        // 先把记录写到数据库中
        ModelEntity modelEntity = new ModelEntity();
        BeanUtils.copyProperties(modelForm, modelEntity);
        // 计算一下md5
        try (InputStream inputStream = new FileInputStream(file)) {
            modelEntity.setMd5(DigestUtils.md5DigestAsHex(inputStream));
        } catch (IOException e) {
            log.error("e = {}", e);
            return ResultUtils.error(ResultEnum.FILE_IS_WRONG.getCode(), e.getMessage());
        }
        modelEntity = modelRepository.save(modelEntity);

        // 通过数据库记录生成新的docker-compose.yml文件
        try {
            dockerService.generateDockerComposeYml();
            if (active.equalsIgnoreCase("dev")) {
                // 开发版本还要拷贝docker-compose.yml文件
                String cmd = remoteService.createScpCommand(customConfig.getDockerComposePath(), "/opt/docker/algorithm-manage-platform");
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            }
        } catch (IOException | InterruptedException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.CMD_ERROR);
        }

        // 让模型对应的docker容器先跑起来
        try {
            dockerService.createDocker(modelForm.getName());
        } catch (Exception e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.CMD_ERROR);
        }

        // mleap还要额外让模型上线
        if (modelForm.getType() == ModelTypeEnum.MLEAP.getCode()) {
            try {
                String result = mLeapService.online(modelForm.getName());
            } catch (Exception e) {
                log.error("e = {}", e);
                if (file.exists()) {
                    file.delete();
                }
                return ResultUtils.error(ResultEnum.MODEL_ONLINE_FAILED.getCode(), e.getMessage());
            }
        }

        return ResultUtils.success(modelEntity);
    }

    /**
     * 根据id或name删除模型
     * @param id
     * @return
     */
    @PostMapping("/delModel")
    public ResultVO delModel(String id, String name) {
        // 先根据ID找到模型实体
        ModelEntity modelEntity = getModelEntityByIdOrName(id, name);

        // 让docker下线
        try {
            dockerService.deleteDocker(modelEntity.getName());
        } catch (Exception e) {
            log.error("e = {}", e);
        }

        // 删除数据库中的记录
        modelRepository.delete(modelEntity);

        // 重新生成docker-compose.yml
        try {
            dockerService.generateDockerComposeYml();
        } catch (IOException e) {
            log.error("e = {}", e);
        }
        return ResultUtils.success();
    }

    /**
     * 根据id或name修改模型
     * 前端是只允许修改模型描述，他要改其它参数，就先删除再重新添加
     *
     * @param id
     * @param desc
     * @return
     */
    @PostMapping("/updateModel")
    public ResultVO updateModel(String id, String name, String desc) {
        ModelEntity entity = getModelEntityByIdOrName(id, name);
        entity.setDesc(desc);
        entity = modelRepository.save(entity);
        return ResultUtils.success(entity);
    }

    private ModelEntity getModelEntityByIdOrName(String id, String name) {
        ModelEntity entity;
        if (!StringUtils.isEmpty(id)) {
            entity = modelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        } else if (!StringUtils.isEmpty(name)) {
            entity = modelRepository.findByName(name).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        } else {
            throw new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR);
        }
        return entity;
    }

    /**
     * 获取所有模型类型
     *
     * @return
     */
    @GetMapping("/getAllModelType")
    public ResultVO getAllModelType() {
        List<Map<Integer, String>> mapList = new ArrayList<>();
        for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
            Map<Integer, String> map = new HashMap<>();
            map.put(modelTypeEnum.getCode(), modelTypeEnum.getDesc());
            mapList.add(map);
        }
        return ResultUtils.success(mapList);
    }

    /**
     * 给easyui提供模型类型列表
     * @return
     */
    @GetMapping("/getModelTypeList")
    public List<Map<String, Object>> getModelTypeList() {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("code", modelTypeEnum.getCode());
            map.put("desc", modelTypeEnum.getDesc());
            mapList.add(map);
        }
        return mapList;
    }

    /**
     * 获取某个模型类型下面的所有模型参数
     *
     * @return
     */
    @GetMapping("/getAllModelParam")
    public ResultVO getAllModelParam(int modelTypeCode) {
        List<Map<Integer, String>> mapList = new ArrayList<>();
        ModelParamEnum[] modelParamEnums = ModelParamEnum.values();
        for (ModelParamEnum modelParamEnum : modelParamEnums) {
            if (modelParamEnum.getModelTypeCode() == modelTypeCode) {
                Map<Integer, String> map = new HashMap<>();
                map.put(modelParamEnum.getCode(), modelParamEnum.getDesc());
                mapList.add(map);
            }
        }
        return ResultUtils.success(mapList);
    }

    /**
     * 给easyui提供模型参数列表
     * @return
     */
    @GetMapping("/getModelParamList")
    public List<Map<String, Object>> getModelParamList(int typeCode) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (ModelParamEnum modelParamEnum : ModelParamEnum.values()) {
            if (modelParamEnum.getModelTypeCode() == typeCode) {
                Map<String, Object> map = new HashMap<>();
                map.put("code", modelParamEnum.getCode());
                map.put("desc", modelParamEnum.getDesc());
                mapList.add(map);
            }
        }
        return mapList;
    }
}
