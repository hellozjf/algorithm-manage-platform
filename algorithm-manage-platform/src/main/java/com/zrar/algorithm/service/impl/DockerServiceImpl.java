package com.zrar.algorithm.service.impl;

import cn.hutool.core.util.RuntimeUtil;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.constant.StateEnum;
import com.zrar.algorithm.domain.AiModelEntity;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.repository.AiModelRepository;
import com.zrar.algorithm.service.*;
import com.zrar.algorithm.vo.FullNameVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
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
    private AiModelRepository aiModelRepository;

    @Autowired
    private MLeapService mLeapService;

    @Autowired
    private FileService fileService;

    @Autowired
    private RemoteService remoteService;

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ContainerService containerService;

    @Value("${spring.profiles.active}")
    private String active;

    @Value("${custom.model-outer-path}")
    private String modelOuterPath;

    /**
     * 是否已经启动
     */
    private boolean isStarted;

    @Override
    public void init() {

        isStarted = false;

        try {
            // 创建宿主机模型文件夹
            File folder = new File(modelOuterPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            // 开发环境，在开发服务器上新建模型文件夹，同时把模型拷贝到开发服务器上面去
            if (active.equalsIgnoreCase("dev")) {
                String cmd = remoteService.createExecCommand("mkdir -p /opt/docker/algorithm-manage-platform/models");
                String result = RuntimeUtil.execForStr(cmd);
                log.debug("{} return {}", cmd, result);

                // todo 这里要验证一下，如果当前这个模型正在被使用，这样拷贝有没有什么问题
                cmd = remoteService.createScpRCommand(modelOuterPath + "/*", "/opt/docker/algorithm-manage-platform/models/");
                result = RuntimeUtil.execForStr(cmd);
                log.debug("{} return {}", cmd, result);
            }

            // 解压tensorflow模型
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    String fullName = file.getName().split("\\.")[0];
                    log.debug("shortName = {}", fullName);
                    // 从完整的名称中拆分出 前缀-类型-名称-版本
                    FullNameVO containerNameVO = FullNameVO.getByFullName(fullName);
                    // 判断数据库中是否有对应的记录
                    if (!aiModelRepository.findByTypeAndShortNameAndVersion(
                            containerNameVO.getIType(),
                            containerNameVO.getShortName(),
                            containerNameVO.getVersion()).isPresent()) {
                        log.error("找不到文件{}对应的数据库记录，这可能是个脏文件", file.getName());
                        continue;
                    }
                    // 有的话把该记录取出来
                    AiModelEntity modelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                            containerNameVO.getIType(),
                            containerNameVO.getShortName(),
                            containerNameVO.getVersion()
                    ).get();
                    // 如果是tensorflow类型的模型，还需要进行解压
                    if (modelEntity.getType() == ModelTypeEnum.TENSORFLOW.getCode()) {
                        unpackModel(fullName);
                    }
                }
            }

            // 将当前容器的状态与数据库的记录进行同步
            List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
            List<AiModelEntity> aiModelEntityList = aiModelRepository.findAll();
            for (AiModelEntity aiModelEntity : aiModelEntityList) {
                boolean bFind = false;
                FullNameVO fullNameVO = containerService.getFullName(aiModelEntity);
                for (Container container : containerList) {
                    if (isContainerNameEquals(container, fullNameVO.getFullName())) {
                        bFind = true;
                        if (!aiModelEntity.getState().equalsIgnoreCase(container.state())) {
                            // 数据库中的状态与实际容器的状态不一致
                            if (aiModelEntity.getState().equalsIgnoreCase(StateEnum.RUNNING.getState())) {
                                dockerClient.startContainer(container.id());
                            } else {
                                // 若一直停止不了，10秒后kill掉它
                                dockerClient.stopContainer(container.id(), 10);
                            }
                        }
                        break;
                    }
                }
                if (!bFind) {
                    // 数据库中有，但是容器中没有，那么就要重新创建一个容器
                    ContainerCreation container = createDocker(fullNameVO.getFullName());
                    if (aiModelEntity.getState().equalsIgnoreCase(StateEnum.RUNNING.getState())) {
                        dockerClient.startContainer(container.id());
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
     * @param container
     * @param fullName
     * @return
     */
    private boolean isContainerNameEquals(Container container, String fullName) {
        String containerName = container.names().get(0);
        return containerName.equalsIgnoreCase(fullName);
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void unpackModel(String fullName) {
        if (active.equalsIgnoreCase("dev")) {
            // 开发环境，进入远程服务器，解压模型
            String cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform/models/; unzip -o " + fullName + ".zip -d " + fullName);
            String result = RuntimeUtil.execForStr(cmd);
            log.debug("{} return {}", cmd, result);
        } else {
            // 生产环境，进入宿主机模型目录，解压模型
            String cmd = "unzip -o " + modelOuterPath + "/" + fullName + ".zip -d " + modelOuterPath + "/" + fullName;
            String result = RuntimeUtil.execForStr(cmd);
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
     * 创建docker容器
     *
     * @param fullName
     * @throws Exception
     */
    @Override
    public ContainerCreation createDocker(String fullName) throws Exception {
        deleteDocker(fullName);

        // 首先查出待创建的容器所需的镜像和端口
        FullNameVO fullNameVO = FullNameVO.getByFullName(fullName);
        String image = null;
        int port = -1;
        if (fullNameVO.getIType() == ModelTypeEnum.MLEAP.getCode()) {
            image = imageService.getMleap();
            port = 65327;
        } else if (fullNameVO.getIType() == ModelTypeEnum.TENSORFLOW.getCode()) {
            image = imageService.getTensorflow();
            port = 8501;
        }

        if (image != null) {
            // 为容器绑定一个宿主机的随机端口
            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            List<PortBinding> randomPort = new ArrayList<>();
            randomPort.add(PortBinding.randomPort("0.0.0.0"));
            portBindings.put(String.valueOf(port), randomPort);

            HostConfig hostConfig = HostConfig.builder()
                    .appendBinds("./models/" + fullName + ":/models/" + fullName)
                    .portBindings(portBindings)
                    .build();
            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(image)
                    .env("MODEL_NAME=" + fullName)
                    .hostConfig(hostConfig)
                    .build();

            ContainerCreation container = dockerClient.createContainer(containerConfig, fullName);

            // 这里还需要把端口同步到数据库中
            AiModelEntity aiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                    fullNameVO.getIType(),
                    fullNameVO.getShortName(),
                    fullNameVO.getVersion()).orElseThrow(() -> new AlgorithmException(ResultEnum.JSON_ERROR));
            aiModelEntity.setPort(Integer.valueOf(randomPort.get(0).hostPort()));
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
        List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container : containerList) {
            if (isContainerNameEquals(container, fullName)) {
                dockerClient.startContainer(container.id());

                FullNameVO fullNameVO = FullNameVO.getByFullName(fullName);
                if (fullNameVO.getIType() == ModelTypeEnum.MLEAP.getCode()) {
                    // 还要把mleap模型上线
                    String modelPath = fileService.getModelOutterPath(fullName);
                    File modelFile = new File(modelPath);
                    if (modelFile.exists()) {
                        log.debug("开始恢复{}", fullName);
                        mLeapService.online(fullName);
                    }
                }

                break;
            }
        }
    }

    @Override
    public void stopDocker(String fullName) throws Exception {
        List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container : containerList) {
            if (isContainerNameEquals(container, fullName)) {
                dockerClient.stopContainer(container.id(), 10);
                break;
            }
        }
    }

    /**
     * 根据fullName删除容器
     * @param fullName
     * @throws Exception
     */
    @Override
    public void deleteDocker(String fullName) throws Exception {
        List<Container> containerList = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container : containerList) {
            if (isContainerNameEquals(container, fullName)) {
                dockerClient.stopContainer(container.id(), 10);
                dockerClient.removeContainer(container.id());
                break;
            }
        }
    }
}
