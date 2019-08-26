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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
     *
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
     * 打开编辑窗口的时候，需要获取详情
     *
     * @param id
     * @param name
     * @return
     */
    @GetMapping("/getModel")
    public ResultVO getModel(String id, String name) {
        ModelEntity entity = getModelEntityByIdOrName(id, name);
        return ResultUtils.success(entity);
    }

    /**
     * 重启单个容器
     * @param id
     * @return
     */
    @PostMapping("/restartModel")
    public ResultVO restartModel(String id) {
        ModelEntity modelEntity = modelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        dockerService.restartDocker(modelEntity.getName());
        return ResultUtils.success();
    }

    /**
     * 增加一键重启功能，避免docker容器发生异常导致服务不正常
     *
     * @return
     */
    @RequestMapping("/reboot")
    public ResultVO reboot() {
        dockerService.init();
        return ResultUtils.success();
    }

    /**
     * 这个controller只有当docker相关服务全部正常启动之后才返回，避免用户刷新了页面又能进行相关操作
     * @return
     */
    @GetMapping("/waitForStarted")
    public ResultVO waitForStarted() {
        while (! dockerService.isStarted()) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                log.error("e = {}", e);
            }
        }
        return ResultUtils.success();
    }

    /**
     * 判断docker服务是否已经正常启动
     * @return
     */
    @GetMapping("/isStarted")
    public ResultVO isStarted() {
        return ResultUtils.success(dockerService.isStarted());
    }

    /**
     * 保存文件
     */
    private void saveFile(MultipartFile multipartFile,
                          File file,
                          ModelForm modelForm) {

        log.debug("saveFile");

        // 保存前，如果待保存文件存在的话，需要删除对应的文件
        if (file.exists()) {
            file.delete();
            if (modelForm.getType() == ModelTypeEnum.TENSORFLOW.getCode()) {
                // tensorflow模型还需要删除文件夹
                if (active.equalsIgnoreCase("dev")) {
                    // 开发版需要进入远程服务器，执行删除命令
                    try {
                        String cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform/models; rm -rf " + modelForm.getName());
                        Process process = runtime.exec(cmd);
                        log.debug("{} return {}", cmd, process.waitFor());
                    } catch (Exception e) {
                        log.error("e = {}", e);
                        throw new AlgorithmException(ResultEnum.CMD_ERROR);
                    }
                } else {
                    // 生产版需要执行删除命令
                    try {
                        String cmd = "rm -rf " + customConfig.getModelOuterPath() + "/" + modelForm.getName();
                        Process process = runtime.exec(cmd);
                        log.debug("{} return {}", cmd, process.waitFor());
                    } catch (Exception e) {
                        log.error("e = {}", e);
                        throw new AlgorithmException(ResultEnum.CMD_ERROR);
                    }
                }
            }
        }

        // 拷贝文件，拷贝的时候如果出错了，需要抛异常
        try (InputStream inputStream = multipartFile.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.FILE_IS_WRONG.getCode(), e.getMessage());
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
                dockerService.unpackModel(modelForm.getName());
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.CMD_ERROR);
            }
        }
    }

    private ModelEntity saveToDatabase(ModelForm modelForm, boolean isCreate, File file) {

        log.debug("saveToDatabase");

        ModelEntity modelEntity = createOrUpdateModelEntity(modelForm, isCreate);
        // 计算一下md5
        try (InputStream inputStream = new FileInputStream(file)) {
            modelEntity.setMd5(DigestUtils.md5DigestAsHex(inputStream));
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.FILE_IS_WRONG.getCode(), e.getMessage());
        }
        modelEntity = modelRepository.save(modelEntity);
        return modelEntity;
    }

    private ModelEntity saveToDatabase(ModelForm modelForm, boolean isCreate) {

        log.debug("saveToDatabase");

        ModelEntity modelEntity = createOrUpdateModelEntity(modelForm, isCreate);
        modelEntity = modelRepository.save(modelEntity);
        return modelEntity;
    }

    private ModelEntity createOrUpdateModelEntity(ModelForm modelForm, boolean isCreate) {
        ModelEntity modelEntity;
        if (isCreate) {
            modelEntity = new ModelEntity();
            BeanUtils.copyProperties(modelForm, modelEntity);
            modelEntity.setId(null);
        } else {
            modelEntity = modelRepository.findById(modelForm.getId()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            Date gmtCreate = modelEntity.getGmtCreate();
            BeanUtils.copyProperties(modelForm, modelEntity);
            modelEntity.setGmtCreate(gmtCreate);
        }
        return modelEntity;
    }

    private void generateDockerComposeYml() {

        log.debug("generateDockerComposeYml");

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
    }

    private void onlineMLeap(ModelForm modelForm) {

        if (modelForm.getType() == ModelTypeEnum.MLEAP.getCode()) {

            log.debug("onlineMLeap");

            try {
                String result = mLeapService.online(modelForm.getName());
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.MODEL_ONLINE_FAILED.getCode(), e.getMessage());
            }
        }
    }

    private void deleteDocker(String name) {
        try {
            log.debug("正在停止{}容器", name);
            dockerService.deleteDocker(name);
        } catch (Exception e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.CMD_ERROR);
        }
    }

    private void createDocker(String name) {
        try {
            log.debug("正在启动{}容器", name);
            dockerService.createDocker(name);
        } catch (Exception e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.CMD_ERROR);
        }
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

        // 组合类型的记录，只添加数据库记录
        if (modelForm.getType() == ModelTypeEnum.COMPOSE.getCode()) {
            ModelEntity modelEntity = saveToDatabase(modelForm, true);
            return ResultUtils.success(modelEntity);
        }

        // 判断上传过来的文件是不是空的
        if (multipartFile.isEmpty()) {
            return ResultUtils.error(ResultEnum.FILE_CAN_NOT_BE_EMPTY);
        }

        // 获取要保存的文件
        File file = new File(fileService.getModelOutterPath(modelForm.getName()));

        // 将上传上来的文件保存到 customConfig.modelOuterPath 目录下
        saveFile(multipartFile, file, modelForm);

        // 把记录写到数据库中
        ModelEntity modelEntity = saveToDatabase(modelForm, true, file);

        // 通过数据库记录生成新的docker-compose.yml文件
        generateDockerComposeYml();

        // 让模型对应的docker容器先跑起来
        createDocker(modelForm.getName());

        // mleap还要额外让模型上线
        onlineMLeap(modelForm);

        return ResultUtils.success(modelEntity);
    }

    /**
     * 根据id删除模型
     *
     * @param ids 逗号隔开的id字符串
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/delModel")
    public ResultVO delModel(String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            ModelEntity modelEntity = modelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            deleteDocker(modelEntity.getName());
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

    @RequestMapping("/downloadModel")
    public ResponseEntity<byte[]> downloadModel(String id) {

        // 将模型文件读取到byte数组中
        ModelEntity modelEntity = modelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        String modelFilePath = fileService.getModelOutterPath(modelEntity.getName());
        File file = new File(modelFilePath);
        byte[] bytes = null;
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(fileInputStream, byteArrayOutputStream);
            bytes = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("e = {}", e.getMessage());
        }

        // 告诉浏览器，以附件的形式下载
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDispositionFormData("attachment", modelEntity.getName() + ".zip");
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.CREATED);
    }

    /**
     * 根据id修改模型
     *
     * @return
     */
    @PostMapping("/modifyModel")
    public ResultVO modifyModel(@RequestParam("file") MultipartFile multipartFile,
                                ModelForm modelForm) {

        // 组合类型的记录，只添加数据库记录
        if (modelForm.getType() == ModelTypeEnum.COMPOSE.getCode()) {
            ModelEntity modelEntity = saveToDatabase(modelForm, false);
            return ResultUtils.success(modelEntity);
        }

        // 获取要保存的文件
        File file = new File(fileService.getModelOutterPath(modelForm.getName()));

        ModelEntity modelEntity = null;

        // 判断上传过来的文件是不是空的
        if (multipartFile.isEmpty()) {
            // 只需要修改数据库即可
            modelEntity = saveToDatabase(modelForm, false, file);
        } else {
            // 让模型对应的docker容器先停止
            deleteDocker(modelForm.getName());
            // 将上传上来的文件保存到 customConfig.modelOuterPath 目录下
            saveFile(multipartFile, file, modelForm);
            // 把记录写到数据库中
            modelEntity = saveToDatabase(modelForm, false, file);
            // 通过数据库记录生成新的docker-compose.yml文件
            generateDockerComposeYml();
            // 让模型对应的docker容器先跑起来
            createDocker(modelForm.getName());
            // mleap还要额外让模型上线
            onlineMLeap(modelForm);
        }

        return ResultUtils.success(modelEntity);
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
     *
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
     *
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
