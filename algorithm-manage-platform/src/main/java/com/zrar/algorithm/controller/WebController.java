package com.zrar.algorithm.controller;

import com.zrar.algorithm.config.CustomConfig;
import com.zrar.algorithm.constant.ModelParamEnum;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.domain.ModelEntity;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.form.ModelForm;
import com.zrar.algorithm.repository.ModelRepository;
import com.zrar.algorithm.service.*;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    private TensorflowService tensorflowService;

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
    public ResultVO getAllModels() {
        List<ModelEntity> modelEntityList = modelRepository.findAll();
        return ResultUtils.success(modelEntityList);
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
                             @Valid ModelForm modelForm) {

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
        modelRepository.save(modelEntity);

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
     * 删除模型
     *
     * @param modelEntity
     * @return
     */
    @PostMapping("/delModel")
    public ResultVO delModel(ModelEntity modelEntity) {
        // 先让docker下线
        try {
            dockerService.deleteDocker(modelEntity.getName());
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        // 删除数据库中的记录
        ModelEntity entity = modelRepository.findById(modelEntity.getId()).get();
        if (entity != null) {
            modelRepository.delete(modelEntity);
        }
        // 重新生成docker-compose.yml
        try {
            dockerService.generateDockerComposeYml();
        } catch (IOException e) {
            log.error("e = {}", e);
        }
        return ResultUtils.success();
    }

    /**
     * 修改模型
     * 前端是只允许修改模型描述，他要改其它参数，就先删除再重新添加
     *
     * @param id
     * @param desc
     * @return
     */
    @PostMapping("/updateModel")
    public ResultVO updateModel(String id, String desc) {
        ModelEntity entity = modelRepository.findById(id).get();
        entity.setDesc(desc);
        modelRepository.save(entity);
        return ResultUtils.success(entity);
    }

    /**
     * 获取所有模型类型
     * @return
     */
    @GetMapping("/getAllModelType")
    public ResultVO getAllModelType() {
        List<ModelTypeEnum> modelTypeEnumList = new ArrayList<>();
        for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
            modelTypeEnumList.add(modelTypeEnum);
        }
        return ResultUtils.success(modelTypeEnumList);
    }

    /**
     * 获取某个模型类型下面的所有模型参数
     *
     * @return
     */
    @GetMapping("/getAllModelParam")
    public ResultVO getAllModelParam(int modelTypeCode) {
        List<ModelParamEnum> modelParamEnumList = new ArrayList<>();
        ModelParamEnum[] modelParamEnums = ModelParamEnum.values();
        for (ModelParamEnum modelParamEnum : modelParamEnums) {
            if (modelParamEnum.getModelTypeCode() == modelTypeCode) {
                modelParamEnumList.add(modelParamEnum);
            }
        }
        return ResultUtils.success(modelParamEnumList);
    }
}
