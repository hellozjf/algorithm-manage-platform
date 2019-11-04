package com.zrar.algorithm.controller;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.digest.Digester;
import com.zrar.algorithm.config.CustomDockerConfig;
import com.zrar.algorithm.config.CustomWorkdirConfig;
import com.zrar.algorithm.constant.ModelParamEnum;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.constant.StateEnum;
import com.zrar.algorithm.domain.AiModelEntity;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.form.ModelForm;
import com.zrar.algorithm.repository.AiModelRepository;
import com.zrar.algorithm.service.*;
import com.zrar.algorithm.util.ModelParamUtils;
import com.zrar.algorithm.util.ModelTypeUtils;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.FullNameVO;
import com.zrar.algorithm.vo.ModelVO;
import com.zrar.algorithm.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    private AiModelRepository aiModelRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private MLeapService mLeapService;

    @Autowired
    private RemoteService remoteService;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private FullNameService fullNameService;

    @Autowired
    private CustomDockerConfig customDockerConfig;

    @Autowired
    private CustomWorkdirConfig customWorkdirConfig;

    @Autowired
    private Digester md5;

    /**
     * 获取所有的模型
     *
     * @return
     */
    @GetMapping("/getAllModels")
    public ResultVO getAllModels(Pageable pageable) {
        Page<AiModelEntity> modelEntityPage = aiModelRepository.findAll(pageable);
        List<AiModelEntity> modelEntityList = modelEntityPage.getContent();
        List<ModelVO> modelVOList = getModelVOList(modelEntityList);
        Page<ModelVO> modelVOPage = new PageImpl<>(modelVOList, modelEntityPage.getPageable(), modelEntityPage.getTotalElements());
        return ResultUtils.success(modelVOPage);
    }

    private List<ModelVO> getModelVOList(List<AiModelEntity> modelEntityList) {
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
        List<AiModelEntity> modelEntityList = aiModelRepository.findAll();
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
     * @param fullName
     * @return
     */
    @GetMapping("/getModel")
    public ResultVO getModel(String id, String fullName) {
        AiModelEntity entity = getModelEntityByIdOrFullName(id, fullName);
        return ResultUtils.success(entity);
    }

    /**
     * 重启单个容器
     * @param id
     * @return
     */
    @PostMapping("/restartModel")
    public ResultVO restartModel(String id) {
        AiModelEntity modelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getFullNameByAiModelEntity(modelEntity);
        try {
            dockerService.restartDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            ResultUtils.error(ResultEnum.RESTART_DOCKER_ERROR);
        }
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
                          FullNameVO fullNameVO) {

        log.debug("saveFile");

        // 保存前，如果待保存文件存在的话，需要删除对应的文件
        if (file.exists()) {
            file.delete();
            if (fullNameVO.getIType() == ModelTypeEnum.TENSORFLOW.getCode()) {
                // tensorflow模型还需要删除文件夹
                if (customWorkdirConfig.isNeedCopy()) {
                    // 开发版需要进入远程服务器，执行删除命令
                    try {
                        String cmd = remoteService.createExecCommand("cd " + customDockerConfig.getModelOutter() + "; rm -rf " + fullNameVO.getFullName());
                        String result = RuntimeUtil.execForStr(cmd);
                        log.debug("{} return {}", cmd, result);
                    } catch (Exception e) {
                        log.error("e = {}", e);
                        throw new AlgorithmException(ResultEnum.CMD_ERROR);
                    }
                } else {
                    // 生产版需要执行删除命令
                    try {
                        String cmd = "rm -rf " + customDockerConfig.getModelOutter() + "/" + fullNameVO.getFullName();
                        String result = RuntimeUtil.execForStr(cmd);
                        log.debug("{} return {}", cmd, result);
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
        if (customWorkdirConfig.isNeedCopy()) {
            try {
                String cmd = remoteService.createScpCommand(fileService.getModelPath(fullNameVO.getFullName()),
                        customDockerConfig.getModelOutter());
                String result = RuntimeUtil.execForStr(cmd);
                log.debug("{} return {}", cmd, result);
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.CMD_ERROR);
            }
        }

        // 如果是tensorflow模型，还需要将模型zip进行解压
        if (fullNameVO.getIType() == ModelTypeEnum.TENSORFLOW.getCode()) {
            try {
                dockerService.unpackModel(fullNameVO.getFullName());
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.CMD_ERROR);
            }
        }
    }

    private AiModelEntity saveToDatabase(ModelForm modelForm, boolean isCreate, File file) {

        log.debug("saveToDatabase");

        AiModelEntity modelEntity = createOrUpdateModelEntity(modelForm, isCreate);
        // 计算一下md5
        try (InputStream inputStream = new FileInputStream(file)) {
            modelEntity.setMd5(DigestUtils.md5DigestAsHex(inputStream));
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.FILE_IS_WRONG.getCode(), e.getMessage());
        }
        modelEntity = aiModelRepository.save(modelEntity);
        return modelEntity;
    }

    private AiModelEntity createOrUpdateModelEntity(ModelForm modelForm, boolean isCreate) {
        AiModelEntity aiModelEntity;
        if (isCreate) {
            aiModelEntity = new AiModelEntity();
            BeanUtils.copyProperties(modelForm, aiModelEntity);
            aiModelEntity.setId(null);
        } else {
            aiModelEntity = aiModelRepository.findById(modelForm.getId()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            Date gmtCreate = aiModelEntity.getGmtCreate();
            BeanUtils.copyProperties(modelForm, aiModelEntity);
            aiModelEntity.setGmtCreate(gmtCreate);
        }
        return aiModelEntity;
    }

    private void onlineMLeap(ModelForm modelForm) {

        if (modelForm.getType() == ModelTypeEnum.MLEAP.getCode()) {

            log.debug("onlineMLeap");

            try {
                String result = mLeapService.online(modelForm.getShortName());
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

        // 获取模型名称，模型类型，是否更新版本
        String shortName = modelForm.getShortName();
        int type = modelForm.getType().intValue();
        int version = modelForm.getVersion().intValue();
        boolean bRenewVersion = modelForm.getBNewVersion().booleanValue();

        // 获取新的aiModelEntity
        AiModelEntity aiModelEntity = getAiModelEntity(modelForm, shortName, type, version, bRenewVersion);
        FullNameVO fullNameVO = fullNameService.getFullNameByAiModelEntity(aiModelEntity);

        // 组合类型的记录，只添加数据库记录
        if (modelForm.getType() == ModelTypeEnum.COMPOSE.getCode()) {
            AiModelEntity modelEntity = aiModelRepository.save(aiModelEntity);
            return ResultUtils.success(modelEntity);
        }

        // 判断上传过来的文件是不是空的
        if (multipartFile.isEmpty()) {
            throw new AlgorithmException(ResultEnum.FILE_CAN_NOT_BE_EMPTY);
        }

        // 获取要保存的文件
        File file = new File(fileService.getModelOutterPath(fullNameVO.getFullName()));
        // 将上传上来的文件保存到 model 目录下
        saveFile(multipartFile, file, fullNameVO);
        // 更新文件md5
        aiModelEntity.setMd5(md5.digestHex(file));

        // 把记录写到数据库中
        aiModelEntity = aiModelRepository.save(aiModelEntity);

        // 首先创建模型对应的docker容器
        try {
            dockerService.createDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.CREATE_DOCKER_ERROR);
        }

        if (aiModelEntity.getState().equalsIgnoreCase(StateEnum.RUNNING.getState())) {
            // 如果模型是需要启动的，再启动它
            try {
                dockerService.startDocker(fullNameVO.getFullName());
            } catch (Exception e) {
                log.error("e = ", e);
                throw new AlgorithmException(ResultEnum.START_DOCKER_ERROR);
            }
        }

        return ResultUtils.success(aiModelEntity);
    }

    private AiModelEntity getAiModelEntity(@Valid ModelForm modelForm, String shortName, int type, int version, boolean bRenewVersion) {
        AiModelEntity aiModelEntity;
        if (bRenewVersion) {
            // 如果要更新版本号，那么根据shortName和type去数据库查找最新的记录
            Optional<AiModelEntity> optionalAiModelEntity = aiModelRepository.findTopByTypeAndShortNameOrderByVersionDesc(type, shortName);
            if (optionalAiModelEntity.isPresent()) {
                // 更新版本号
                aiModelEntity = optionalAiModelEntity.get();
                aiModelEntity.setVersion(aiModelEntity.getVersion() + 1);
            } else {
                // 新建一个
                aiModelEntity = new AiModelEntity();
                BeanUtils.copyProperties(modelForm, aiModelEntity);
                aiModelEntity.setVersion(1);
            }
        } else {
            Optional<AiModelEntity> optionalAiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(type, shortName, version);
            if (optionalAiModelEntity.isPresent()) {
                // 获取
                aiModelEntity = optionalAiModelEntity.get();
            } else {
                // 新建一个
                aiModelEntity = new AiModelEntity();
                BeanUtils.copyProperties(modelForm, aiModelEntity);
                aiModelEntity.setVersion(1);
            }
        }
        return aiModelEntity;
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
            AiModelEntity aiModelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            FullNameVO fullNameVO = fullNameService.getFullNameByAiModelEntity(aiModelEntity);
            try {
                dockerService.deleteDocker(fullNameVO.getFullName());
            } catch (Exception e) {
                log.error("e = ", e);
                throw new AlgorithmException(ResultEnum.DELETE_DOCKER_ERROR);
            }
            aiModelRepository.delete(aiModelEntity);
        }
        return ResultUtils.success();
    }

    @RequestMapping("/downloadModel")
    public ResponseEntity<byte[]> downloadModel(String id) {

        // 将模型文件读取到byte数组中
        AiModelEntity aiModelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getFullNameByAiModelEntity(aiModelEntity);
        String modelFilePath = fileService.getModelOutterPath(fullNameVO.getFullName());
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
        httpHeaders.setContentDispositionFormData("attachment", fullNameVO.getFullName() + ".zip");
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.CREATED);
    }

    /**
     * 根据id修改模型
     *
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/modifyModel")
    public ResultVO modifyModel(@RequestParam("file") MultipartFile multipartFile,
                                ModelForm modelForm) {

        // 获取类型，名称，版本号，是否更新版本
        int type = modelForm.getType().intValue();
        String shortName = modelForm.getShortName();
        int version = modelForm.getVersion().intValue();
        boolean bNewVersion = modelForm.getBNewVersion().booleanValue();
        AiModelEntity aiModelEntity = getAiModelEntity(modelForm, shortName, type, version, bNewVersion);
        FullNameVO fullNameVO = fullNameService.getFullNameByAiModelEntity(aiModelEntity);

        // 组合类型的记录，只添加数据库记录
        if (aiModelEntity.getType() == ModelTypeEnum.COMPOSE.getCode()) {
            aiModelEntity = aiModelRepository.save(aiModelEntity);
            return ResultUtils.success(aiModelEntity);
        }

        // 获取要保存的文件
        File file = new File(fileService.getModelOutterPath(fullNameVO.getFullName()));

        // 判断上传过来的文件是不是空的
        if (multipartFile.isEmpty()) {
            // 只需要修改数据库即可
            aiModelEntity = aiModelRepository.save(aiModelEntity);
        } else {
            // 让模型对应的docker容器先删掉
            try {
                dockerService.deleteDocker(fullNameVO.getFullName());
            } catch (Exception e) {
                log.error("e = ", e);
                throw new AlgorithmException(ResultEnum.DELETE_DOCKER_ERROR);
            }
            // 将上传上来的文件保存到 model 目录下
            saveFile(multipartFile, file, fullNameVO);
            // 把记录写到数据库中
            aiModelEntity = aiModelRepository.save(aiModelEntity);
            // 让模型对应的docker容器先创建出来
            try {
                dockerService.createDocker(fullNameVO.getFullName());
            } catch (Exception e) {
                throw new AlgorithmException(ResultEnum.CREATE_DOCKER_ERROR);
            }
            if (aiModelEntity.getState().equalsIgnoreCase(StateEnum.RUNNING.getState())) {
                // 让模型对应的docker容器启动
                try {
                    dockerService.startDocker(fullNameVO.getFullName());
                } catch (Exception e) {
                    throw new AlgorithmException(ResultEnum.START_DOCKER_ERROR);
                }
            }
        }

        return ResultUtils.success(aiModelEntity);
    }

    private AiModelEntity getModelEntityByIdOrFullName(String id, String fullName) {
        AiModelEntity entity;
        if (!StringUtils.isEmpty(id)) {
            entity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        } else if (!StringUtils.isEmpty(fullName)) {
            FullNameVO fullNameVO = fullNameService.getByFullName(fullName);
            entity = aiModelRepository.findByTypeAndShortNameAndVersion(fullNameVO.getIType(), fullNameVO.getShortName(), fullNameVO.getVersion())
                    .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
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
            map.put(modelTypeEnum.getCode(), modelTypeEnum.getDescription());
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
            map.put("desc", modelTypeEnum.getDescription());
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
