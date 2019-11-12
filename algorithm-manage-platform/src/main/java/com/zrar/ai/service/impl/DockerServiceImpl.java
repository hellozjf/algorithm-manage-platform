package com.zrar.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.ZipUtil;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;
import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.config.CustomDockerConfig;
import com.zrar.ai.config.CustomWorkdirConfig;
import com.zrar.ai.constant.DictItem;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.AiModelDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.service.*;
import com.zrar.ai.vo.FullNameVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * docker服务
 *
 * @author Jingfeng Zhou
 */
@Slf4j
@Service
public class DockerServiceImpl implements DockerService {

    @Autowired
    private AiModelDao aiModelRepository;

    @Autowired
    private MLeapService mLeapService;

    @Autowired
    private FileService fileService;

    @Autowired
    private RemoteService remoteService;

    @Autowired
    private FullNameService fullNameService;

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private ImageService imageService;

    @Autowired
    private CustomDockerConfig customDockerConfig;

    @Autowired
    private CustomWorkdirConfig customWorkdirConfig;

    /**
     * 是否已经启动
     */
    private boolean isStarted;

    @Override
    public void init() {

        isStarted = false;

        try {
            // 创建工作主机的模型目录
            File folder = new File(customWorkdirConfig.getModel());
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // 将当前容器的状态与数据库的记录进行同步
            List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
            List<AiModelBO> aiModelEntityList = aiModelRepository.findAll();
            for (AiModelBO aiModelEntity : aiModelEntityList) {
                boolean bFind = false;
                FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelEntity);
                for (Container container : containerList) {
                    if (isContainerNameEquals(container, fullNameVO.getFullName())) {
                        bFind = true;
                        if (!aiModelEntity.getState().equalsIgnoreCase(container.state())) {
                            // 数据库中的状态与实际容器的状态不一致
                            if (aiModelEntity.getState().equalsIgnoreCase(DictItem.MODEL_STATE_RUNNING)) {
                                startDocker(fullNameVO.getFullName());
                            } else {
                                stopDocker(fullNameVO.getFullName());
                            }
                        }
                        break;
                    }
                }
                if (!bFind) {
                    // 数据库中有，但是容器中没有，那么就要重新创建一个容器
                    createDocker(fullNameVO.getFullName());
                    if (aiModelEntity.getState().equalsIgnoreCase(DictItem.MODEL_STATE_RUNNING)) {
                        startDocker(fullNameVO.getFullName());
                    }
                }
            }

        } catch (Exception e) {
            log.error("e = {}", e);
        }

        isStarted = true;
    }

    /**
     * 比较容器名称是否等于fullName
     *
     * @param container
     * @param fullName
     * @return
     */
    private boolean isContainerNameEquals(Container container, String fullName) {
        String containerName = container.names().get(0).substring(1);
        return containerName.equalsIgnoreCase(fullName);
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * 解压zip包
     *
     * @param fullName
     */
    @Override
    public void unpackModel(String fullName) {
        File file = new File(fileService.getModelWorkFilePath(fullName));

        ZipUtil.unzip(file);
        if (customWorkdirConfig.isNeedCopy()) {
            // 首先删除docker宿主机的模型目录
            String cmd = remoteService.createExecCommand("rm -rf " + fileService.getModelOutterFolderPath(fullName));
            String result = RuntimeUtil.execForStr(cmd);
            log.debug("{} return {}", cmd, result);
            // 把解压后的tensorflow模型拷贝到 docker宿主机的模型目录
            cmd = remoteService.createScpRCommand(
                    fileService.getModelWorkFolderPath(fullName),
                    fileService.getModelOutterFolderPath(fullName));
            result = RuntimeUtil.execForStr(cmd);
            log.debug("{} return {}", cmd, result);
        }
    }

    @Override
    public void restartDocker(String fullName) throws Exception {
        stopDocker(fullName);
        startDocker(fullName);
    }

    @Override
    public ContainerCreation recreateDocker(String fullName) throws Exception {
        deleteDocker(fullName);
        return createDocker(fullName);
    }

    /**
     * 从minPort ~ maxPort之间寻找一个未被使用的端口
     *
     * @return
     */
    @Override
    public int getRandomPort() {
        int minPort = customDockerConfig.getPortRangeMin();
        int maxPort = customDockerConfig.getPortRangeMax();
        do {
            int randomPort = RandomUtil.randomInt(minPort, maxPort);
            if (aiModelRepository.findByPort(randomPort).size() == 0) {
                // 找到一个未被使用的端口才返回
                return randomPort;
            }
        } while (true);
    }

    /**
     * 创建docker容器
     *
     * @param fullName
     * @throws Exception
     */
    @Override
    public ContainerCreation createDocker(String fullName) throws Exception {
        deleteDocker(fullName);

        // 首先查出待创建的容器所需的镜像和端口
        FullNameVO fullNameVO = fullNameService.getByFullName(fullName);
        String image = null;
        String folderBindStr = null;
        int port = -1;
        if (fullNameVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_MLEAP)) {
            image = imageService.getMleap();
            port = 65327;
            folderBindStr = customDockerConfig.getModelOutter() + ":/models";
        } else if (fullNameVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_TENSORFLOW)) {
            image = imageService.getTensorflow();
            port = 8501;
            folderBindStr = customDockerConfig.getModelOutter() + "/" + fullName + ":/models/" + fullName;
        }

        if (image != null) {
            // 为容器绑定一个宿主机的随机端口
            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            int randomPort = getRandomPort();
            PortBinding portBinding = PortBinding.of("0.0.0.0", randomPort);
            portBindings.put(String.valueOf(port), Arrays.asList(portBinding));

            HostConfig hostConfig = HostConfig.builder()
                    .appendBinds(folderBindStr)
                    .portBindings(portBindings)
                    .build();
            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(image)
                    .env("MODEL_NAME=" + fullName)
                    .exposedPorts(String.valueOf(port))
                    .hostConfig(hostConfig)
                    .build();

            ContainerCreation container = dockerClient.createContainer(containerConfig, fullName);

            // 这里还需要把端口同步到数据库中
            AiModelBO aiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                    fullNameVO.getType(),
                    fullNameVO.getShortName(),
                    fullNameVO.getVersion()).orElseThrow(() -> new AlgorithmException(ResultEnum.JSON_ERROR));
            aiModelEntity.setPort(randomPort);
            aiModelRepository.save(aiModelEntity);

            return container;
        } else {
            return null;
        }
    }

    /**
     * 启动docker容器
     *
     * @param fullName
     * @throws Exception
     */
    @Override
    public void startDocker(String fullName) throws Exception {
        FullNameVO fullNameVO = fullNameService.getByFullName(fullName);
        List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container : containerList) {
            if (isContainerNameEquals(container, fullName)) {
                // 启动前先要判断环境是否正常
                checkEnvironment(fullNameVO);

                // todo 启动的时候有可能会出现端口冲突，这个时候应该要为它更换一个端口
                dockerClient.startContainer(container.id());
                // todo 如果更换过端口，需要把这个新端口保存到数据库中

                if (fullNameVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_MLEAP)) {
                    // 还要把mleap模型上线
                    log.debug("开始恢复{}", fullName);
                    mLeapService.online(fullName);
                }

                // 数据库修改模型的状态
                AiModelBO aiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                        fullNameVO.getType(),
                        fullNameVO.getShortName(),
                        fullNameVO.getVersion()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
                aiModelEntity.setState(DictItem.MODEL_STATE_RUNNING);
                aiModelRepository.save(aiModelEntity);

                break;
            }
        }
    }

    /**
     * 启动docker容器之前，先要检查环境是否正常
     * 如果没有模型文件，那就直接报错
     * tensorflow模型需要解压
     * 如果需要拷贝，就把模型从 工作主机 拷贝到 docker宿主机
     *
     * @param fullNameVO
     */
    private void checkEnvironment(FullNameVO fullNameVO) {
        if (fullNameVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_MLEAP)) {
            // 检查模型文件是否存在
            File file = new File(fileService.getModelWorkFilePath(fullNameVO.getFullName()));
            if (!file.exists() || !file.isFile()) {
                throw new AlgorithmException(ResultEnum.MODEL_FILE_NOT_EXIST_ERROR);
            }
            if (customWorkdirConfig.isNeedCopy()) {
                // 把模型拷贝到 docker宿主机的模型目录
                String cmd = remoteService.createScpCommand(file.getPath(), fileService.getModelOutterFilePath(fullNameVO.getFullName()));
                String result = RuntimeUtil.execForStr(cmd);
                log.debug("{} return {}", cmd, result);
            }
        } else if (fullNameVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_TENSORFLOW)) {
            // 检查模型文件是否存在
            File file = new File(fileService.getModelWorkFilePath(fullNameVO.getFullName()));
            if (!file.exists() || !file.isFile()) {
                throw new AlgorithmException(ResultEnum.MODEL_FILE_NOT_EXIST_ERROR);
            }
            // 文件夹是否存在，存在的话要删除
            File folder = new File(fileService.getModelWorkFolderPath(fullNameVO.getFullName()));
            if (folder.exists()) {
                FileUtil.del(folder);
            }
            // 解压文件
            unpackModel(fullNameVO.getFullName());
        } else {
            log.error("unknown type {}", fullNameVO.getType());
            throw new AlgorithmException(ResultEnum.UNKNOWN_MODEL_TYPE);
        }
    }

    @Override
    public void stopDocker(String fullName) throws Exception {
        FullNameVO fullNameVO = fullNameService.getByFullName(fullName);
        List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container : containerList) {
            if (isContainerNameEquals(container, fullName)) {
                dockerClient.stopContainer(container.id(), 2);
                if (fullNameVO.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_TENSORFLOW)) {
                    // tensorflow模型就把对应的文件夹删了吧
                    File folder = new File(customWorkdirConfig.getModel(), fullNameVO.getFullName());
                    if (folder.exists()) {
                        FileUtil.del(folder);
                    }
                    if (customWorkdirConfig.isNeedCopy()) {
                        // 把docker宿主机的tensorflow模型目录也删除掉
                        String cmd = remoteService.createExecCommand("rm -rf " + customDockerConfig.getModelOutter() + "/" + fullNameVO.getFullName());
                        String result = RuntimeUtil.execForStr(cmd);
                        log.debug("{} return {}", cmd, result);
                    }
                }

                // 数据库修改模型的状态
                AiModelBO aiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                        fullNameVO.getType(),
                        fullNameVO.getShortName(),
                        fullNameVO.getVersion()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
                aiModelEntity.setState(DictItem.MODEL_STATE_EXISTED);
                aiModelRepository.save(aiModelEntity);

                break;
            }
        }
    }

    /**
     * 根据fullName删除容器
     *
     * @param fullName
     * @throws Exception
     */
    @Override
    public void deleteDocker(String fullName) throws Exception {
        List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container : containerList) {
            if (isContainerNameEquals(container, fullName)) {
                stopDocker(fullName);
                dockerClient.removeContainer(container.id());
                break;
            }
        }
    }
}
