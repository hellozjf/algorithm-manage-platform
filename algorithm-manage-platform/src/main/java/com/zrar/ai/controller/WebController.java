package com.zrar.ai.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.digest.Digester;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zrar.ai.config.CustomDockerConfig;
import com.zrar.ai.config.CustomWorkdirConfig;
import com.zrar.ai.constant.DictItem;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.service.*;
import com.zrar.ai.util.ResultUtils;
import com.zrar.ai.vo.AiModelVO;
import com.zrar.ai.vo.FullNameVO;
import com.zrar.ai.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private FileService fileService;

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
    private Digester md5Digester;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebService webService;

    /**
     * 获取所有的模型
     *
     * @return
     */
    @GetMapping("/getAllModels")
    public ResultVO getAllModels(Pageable pageable) {
        Page<AiModelVO> aiModelEntityPage = webService.getAllModels(pageable);
        return ResultUtils.success(aiModelEntityPage);
    }

    /**
     * 打开编辑窗口的时候，需要获取详情
     *
     * @param id
     * @param fullName
     * @return
     */
    @GetMapping("/getModel")
    public ResultVO getModel(@RequestParam(name = "id", required = false) String id,
                             @RequestParam(name = "fullName", required = false) String fullName) {
        AiModelVO aiModelVO = getModelEntityByIdOrFullName(id, fullName);
        return ResultUtils.success(aiModelVO);
    }

    /**
     * 增加一键重启功能，避免docker容器发生异常导致服务不正常
     *
     * @return
     */
    @GetMapping("/reboot")
    public ResultVO reboot() {
        // 重新创建目前已经存在的所有容器
        List<AiModelVO> aiModelVOList = webService.getAllModels();
        for (AiModelVO aiModelVO : aiModelVOList) {
            FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelVO);
            try {
                dockerService.recreateDocker(fullNameVO.getFullName());
            } catch (Exception e) {
                log.error("e = ", e);
                throw new AlgorithmException(ResultEnum.RECREATE_DOCKER_ERROR);
            }
        }
        // 然后全部初始化一遍
        dockerService.init();
        return ResultUtils.success();
    }

    /**
     * 重启单个容器
     *
     * @param id
     * @return
     */
    @GetMapping("/restartModel")
    public ResultVO restartModel(@RequestParam("id") String id) {
        AiModelVO aiModelVO = webService.findById(id);
        FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelVO);
        try {
            dockerService.restartDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            ResultUtils.error(ResultEnum.RESTART_DOCKER_ERROR);
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
            if (fullNameVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_TENSORFLOW)) {
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
        AiModelVO aiModelVO = webService.findById(id);
        FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelVO);

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
        aiModelVO = webService.updateMd5(aiModelVO, md5Digester.digestHex(file));

        return ResultUtils.success(aiModelVO);
    }

    /**
     * 启动模型
     *
     * @param id
     * @return
     */
    @GetMapping("/startModel")
    public ResultVO startModel(@RequestParam("id") String id) {
        AiModelVO aiModelVO = webService.findById(id);
        FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelVO);
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
        AiModelVO aiModelVO = webService.findById(id);
        FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelVO);
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
        String type = aiModelVO.getType();
        int version = aiModelVO.getVersion().intValue();
        boolean bRenewVersion = aiModelVO.getNewVersion().booleanValue();

        // 获取新的aiModelEntity
        AiModelVO newAiModelVO = webService.getAiModelVO(aiModelVO, shortName, type, version, bRenewVersion);
        FullNameVO fullNameVO = fullNameService.getByAiModel(newAiModelVO);

        // 组合类型的记录，只添加数据库记录
        if (aiModelVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_COMPOSE)) {
            newAiModelVO = webService.save(newAiModelVO);
            return ResultUtils.success(newAiModelVO);
        }

        // 把记录写到数据库中
        newAiModelVO = webService.save(newAiModelVO);

        // 首先创建模型对应的docker容器
        try {
            dockerService.createDocker(fullNameVO.getFullName());
        } catch (Exception e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.CREATE_DOCKER_ERROR);
        }

        return ResultUtils.success(newAiModelVO);
    }


    /**
     * 根据id删除模型
     *
     * @param ids 逗号隔开的id字符串
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/delModel")
    public ResultVO delModel(@RequestParam String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            AiModelVO aiModelVO = webService.findById(id);
            FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelVO);
            try {
                dockerService.deleteDocker(fullNameVO.getFullName());
            } catch (Exception e) {
                log.error("e = ", e);
                throw new AlgorithmException(ResultEnum.DELETE_DOCKER_ERROR);
            }
            webService.delete(aiModelVO);
            // 把模型文件也一起删了
            File file = new File(fileService.getModelWorkFilePath(fullNameVO.getFullName()));
            file.delete();
            if (customWorkdirConfig.isNeedCopy()) {
                String cmd = remoteService.createExecCommand("rm -f " + fileService.getModelOutterFilePath(fullNameVO.getFullName()));
                String result = RuntimeUtil.execForStr(cmd);
                log.error("{} return {}", cmd, result);
            }
        }
        return ResultUtils.success();
    }

    @GetMapping("/downloadFile")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("id") String id) {

        // 将模型文件读取到byte数组中
        AiModelVO aiModelVO = webService.findById(id);
        FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelVO);
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
        String type = aiModelVO.getType();
        String shortName = aiModelVO.getShortName();
        int version = aiModelVO.getVersion().intValue();
        boolean bNewVersion = aiModelVO.getNewVersion().booleanValue();

        // 获取数据库中的实体，并更新它
        AiModelVO newAiModelVO = webService.getAiModelVO(aiModelVO, shortName, type, version, bNewVersion);
        newAiModelVO = webService.save(newAiModelVO);

        if (bNewVersion) {
            // 如果有新版本，需要拷贝模型文件
            FullNameVO oldFullNameVO = fullNameService.getByTypeNameVersion(type, shortName, version);
            FullNameVO newFullNameVO = fullNameService.getByTypeNameVersion(type, shortName, newAiModelVO.getVersion());
            File oldFile = new File(fileService.getModelWorkFilePath(oldFullNameVO.getFullName()));
            File newFile = new File(fileService.getModelWorkFilePath(newFullNameVO.getFullName()));
            if (oldFile.exists()) {
                FileUtil.copy(oldFile, newFile, true);
            }
        }
        return ResultUtils.success(newAiModelVO);
    }

    private AiModelVO getModelEntityByIdOrFullName(String id, String fullName) {
        AiModelVO aiModelVO;
        if (!StringUtils.isEmpty(id)) {
            aiModelVO = webService.findById(id);
        } else if (!StringUtils.isEmpty(fullName)) {
            FullNameVO fullNameVO = fullNameService.getByFullName(fullName);
            aiModelVO = webService.findByTypeAndShortNameAndVersion(fullNameVO.getType(), fullNameVO.getShortName(), fullNameVO.getVersion());
        } else {
            throw new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR);
        }
        return aiModelVO;
    }

    /**
     * 获取所有模型类型
     *
     * @return
     */
    @GetMapping("/getAllModelType")
    public ResultVO getAllModelType() {
        List<Map<Integer, String>> mapList = new ArrayList<>();
//        for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
//            Map<Integer, String> map = new HashMap<>();
//            map.put(modelTypeEnum.getCode(), modelTypeEnum.getDescription());
//            mapList.add(map);
//        }
        return ResultUtils.success(mapList);
    }
}
