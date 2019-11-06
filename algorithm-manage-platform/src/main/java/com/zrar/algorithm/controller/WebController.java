package com.zrar.algorithm.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.digest.Digester;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zrar.algorithm.config.CustomDockerConfig;
import com.zrar.algorithm.config.CustomWorkdirConfig;
import com.zrar.algorithm.constant.ModelParamEnum;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.constant.StateEnum;
import com.zrar.algorithm.domain.AiModelEntity;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.repository.AiModelRepository;
import com.zrar.algorithm.service.*;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.AiModelVO;
import com.zrar.algorithm.vo.FullNameVO;
import com.zrar.algorithm.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取所有的模型
     *
     * @return
     */
    @GetMapping("/getAllModels")
    public ResultVO getAllModels(Pageable pageable) {
        Page<AiModelEntity> aiModelEntityPage = aiModelRepository.findAll(pageable);
        return ResultUtils.success(aiModelEntityPage);
    }

    /**
     * 获取easyui所需要的模型列表
     *
     * @return
     */
    @GetMapping("/getAllModelList")
    public Map<String, Object> getAllModelList() {
        List<AiModelEntity> aiModelEntityList = aiModelRepository.findAll();
        Map<String, Object> map = new HashMap<>();
        map.put("total", aiModelEntityList.size());
        map.put("rows", aiModelEntityList);
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
    public ResultVO getModel(@RequestParam("id") String id,
                             @RequestParam("fullName") String fullName) {
        AiModelEntity entity = getModelEntityByIdOrFullName(id, fullName);
        return ResultUtils.success(entity);
    }

    /**
     * 重启单个容器
     *
     * @param id
     * @return
     */
    @PostMapping("/restartModel")
    public ResultVO restartModel(@RequestParam("id") String id) {
        AiModelEntity modelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getByAiModelEntity(modelEntity);
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
    @GetMapping("/reboot")
    public ResultVO reboot() {
        dockerService.init();
        return ResultUtils.success();
    }

    /**
     * 这个controller只有当docker相关服务全部正常启动之后才返回，避免用户刷新了页面又能进行相关操作
     *
     * @return
     */
    @GetMapping("/waitForStarted")
    public ResultVO waitForStarted() {
        while (!dockerService.isStarted()) {
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
     *
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
                    // 进入docker宿主机，执行删除命令
                    String cmd = remoteService.createExecCommand("rm -rf " + customDockerConfig.getModelOutter() + "/" + fullNameVO.getFullName());
                    String result = RuntimeUtil.execForStr(cmd);
                    log.debug("{} return {}", cmd, result);
                }
                String cmd = "rm -rf " + customWorkdirConfig.getModel() + "/" + fullNameVO.getFullName();
                String result = RuntimeUtil.execForStr(cmd);
                log.debug("{} return {}", cmd, result);
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
    }

    /**
     * 上传模型
     *
     * @param id
     * @param multipartFile
     * @return
     */
    @PostMapping("/uploadFile")
    public ResultVO uploadFile(@RequestParam("id") String id,
                               @RequestParam("file") MultipartFile multipartFile) {

        // 首先去数据库中找到这个模型文件
        AiModelEntity aiModelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getByAiModelEntity(aiModelEntity);

        // 判断上传过来的文件是不是空的
        if (multipartFile.isEmpty()) {
            throw new AlgorithmException(ResultEnum.FILE_CAN_NOT_BE_EMPTY);
        }

        // 获取要保存的文件
        File file = new File(fileService.getModelWorkFilePath(fullNameVO.getFullName()));
        // 保存文件之前，要确保docker容器已经关掉了
        try {
            dockerService.stopDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            throw new AlgorithmException(ResultEnum.STOP_DOCKER_ERROR);
        }
        // 将上传上来的文件保存到 工作主机的模型目录下
        saveFile(multipartFile, file, fullNameVO);
        // 更新文件md5
        aiModelEntity.setMd5(md5.digestHex(file));
        aiModelEntity = aiModelRepository.save(aiModelEntity);
        // 如果模型之前是在运行的，还要再把它启动回来
        if (aiModelEntity.getState().equalsIgnoreCase(StateEnum.RUNNING.getState())) {
            try {
                dockerService.startDocker(fullNameVO.getFullName());
            } catch (Exception e) {
                throw new AlgorithmException(ResultEnum.START_DOCKER_ERROR);
            }
        }

        return ResultUtils.success(aiModelEntity);
    }

    /**
     * 启动模型
     *
     * @param id
     * @return
     */
    @GetMapping("/startModel")
    public ResultVO startModel(@RequestParam("id") String id) {
        AiModelEntity aiModelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getByAiModelEntity(aiModelEntity);
        try {
            dockerService.startDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.STOP_DOCKER_ERROR);
        }
        return ResultUtils.success();
    }

    /**
     * 关闭模型
     *
     * @param id
     * @return
     */
    @GetMapping("/stopModel")
    public ResultVO stopModel(@RequestParam("id") String id) {
        AiModelEntity aiModelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getByAiModelEntity(aiModelEntity);
        try {
            dockerService.stopDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.STOP_DOCKER_ERROR);
        }
        return ResultUtils.success();
    }

    /**
     * 添加模型
     *
     * @param aiModelVO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/addModel")
    public ResultVO addModel(@RequestBody AiModelVO aiModelVO) {

        // 获取模型名称，模型类型，是否更新版本
        String shortName = aiModelVO.getShortName();
        int type = aiModelVO.getType().intValue();
        int version = aiModelVO.getVersion().intValue();
        boolean bRenewVersion = aiModelVO.getNewVersion().booleanValue();

        // 获取新的aiModelEntity
        AiModelEntity aiModelEntity = getAiModelEntity(aiModelVO, shortName, type, version, bRenewVersion);
        // 设置参数
        try {
            aiModelEntity.setParam(objectMapper.writeValueAsString(aiModelVO.getParam()));
        } catch (JsonProcessingException e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
        FullNameVO fullNameVO = fullNameService.getByAiModelEntity(aiModelEntity);

        // 组合类型的记录，只添加数据库记录
        if (aiModelVO.getType() == ModelTypeEnum.COMPOSE.getCode()) {
            AiModelEntity modelEntity = aiModelRepository.save(aiModelEntity);
            return ResultUtils.success(modelEntity);
        }

        // 把记录写到数据库中
        aiModelEntity = aiModelRepository.save(aiModelEntity);

        // 首先创建模型对应的docker容器
        try {
            dockerService.createDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.CREATE_DOCKER_ERROR);
        }

        return ResultUtils.success(aiModelEntity);
    }

    private AiModelEntity getAiModelEntity(AiModelVO aiModelVO, String shortName, int type, int version, boolean bRenewVersion) {
        AiModelEntity aiModelEntity;
        if (bRenewVersion) {
            // 如果要更新版本号，那么根据shortName和type去数据库查找最新的记录
            Optional<AiModelEntity> optionalAiModelEntity = aiModelRepository.findTopByTypeAndShortNameOrderByVersionDesc(type, shortName);
            if (optionalAiModelEntity.isPresent()) {
                AiModelEntity oldAiModelEntity = optionalAiModelEntity.get();

                // 用新版本号创建一个AiModelEntity
                aiModelEntity = new AiModelEntity();
                BeanUtil.copyProperties(aiModelVO, aiModelEntity);
                aiModelEntity.setId(null);
                aiModelEntity.setPort(dockerService.getRandomPort());
                aiModelEntity.setVersion(oldAiModelEntity.getVersion() + 1);
            } else {
                // 新建一个
                aiModelEntity = new AiModelEntity();
                BeanUtils.copyProperties(aiModelVO, aiModelEntity);
                aiModelEntity.setId(null);
                aiModelEntity.setPort(dockerService.getRandomPort());
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
                BeanUtils.copyProperties(aiModelVO, aiModelEntity);
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
    public ResultVO delModel(@RequestBody String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            AiModelEntity aiModelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            FullNameVO fullNameVO = fullNameService.getByAiModelEntity(aiModelEntity);
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

    @GetMapping("/downloadFile")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("id") String id) {

        // 将模型文件读取到byte数组中
        AiModelEntity aiModelEntity = aiModelRepository.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getByAiModelEntity(aiModelEntity);
        String modelFilePath = fileService.getModelOutterFilePath(fullNameVO.getFullName());
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
    public ResultVO modifyModel(@RequestBody AiModelVO aiModelVO) {

        // 获取类型，名称，版本号，是否更新版本
        int type = aiModelVO.getType().intValue();
        String shortName = aiModelVO.getShortName();
        int version = aiModelVO.getVersion().intValue();
        boolean bNewVersion = aiModelVO.getNewVersion().booleanValue();

        // 获取数据库中的实体，并更新它
        AiModelEntity aiModelEntity = getAiModelEntity(aiModelVO, shortName, type, version, bNewVersion);
        try {
            aiModelEntity.setParam(objectMapper.writeValueAsString(aiModelVO.getParam()));
        } catch (JsonProcessingException e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
        aiModelEntity.setDescription(aiModelVO.getDescription());
        aiModelEntity = aiModelRepository.save(aiModelEntity);

        if (bNewVersion) {
            // 如果有新版本，需要拷贝模型文件
            FullNameVO oldFullNameVO = fullNameService.getByTypeNameVersion(type, shortName, version);
            FullNameVO newFullNameVO = fullNameService.getByTypeNameVersion(type, shortName, aiModelEntity.getVersion());
            File oldFile = new File(fileService.getModelWorkFilePath(oldFullNameVO.getFullName()));
            File newFile = new File(fileService.getModelWorkFilePath(newFullNameVO.getFullName()));
            if (oldFile.exists()) {
                FileUtil.copy(oldFile, newFile, true);
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
    public ResultVO getAllModelParam(@RequestParam("modelTypeCode") int modelTypeCode) {
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
