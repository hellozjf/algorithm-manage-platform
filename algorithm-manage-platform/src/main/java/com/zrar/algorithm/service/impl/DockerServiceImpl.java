package com.zrar.algorithm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zrar.algorithm.config.CustomConfig;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.domain.ModelEntity;
import com.zrar.algorithm.dto.DockerComposeDTO;
import com.zrar.algorithm.repository.ModelRepository;
import com.zrar.algorithm.service.DockerService;
import com.zrar.algorithm.service.FileService;
import com.zrar.algorithm.service.MLeapService;
import com.zrar.algorithm.service.RemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
@Service
public class DockerServiceImpl implements DockerService {

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @Autowired
    private MLeapService mLeapService;

    @Autowired
    private Runtime runtime;

    @Autowired
    private FileService fileService;

    @Autowired
    private RemoteService remoteService;

    @Value("${spring.profiles.active}")
    private String active;

    /**
     * 是否已经启动
     */
    private boolean isStarted;

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void init() {

        isStarted = false;

        try {
            // 创建相关文件夹
            File folder = new File(customConfig.getModelOuterPath());
            if (!folder.exists()) {
                folder.mkdirs();
            }
            if (active.equalsIgnoreCase("dev")) {
                String cmd = remoteService.createExecCommand("mkdir -p /opt/docker/algorithm-manage-platform/models");
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            }

            // 删除原有的docker-compose.yml创建的容器
            if (active.equalsIgnoreCase("prod")) {
                String cmd = "docker-compose down";
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            } else if (active.equalsIgnoreCase("dev")) {
                String cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform; docker-compose down");
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            }

            // 如果是dev版本，将模型拷贝到远程服务器
            if (active.equalsIgnoreCase("dev")) {
                // 先把远端的模型全部删了
                String cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform/models; rm -rf *");
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
                // 再把本地的模型全部拷贝到远端
                cmd = remoteService.createScpRCommand(customConfig.getModelOuterPath() + "/*", "/opt/docker/algorithm-manage-platform/models/");
                process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            }

            // 从数据库中生成新的docker-compose.yml文件
            generateDockerComposeYml();
            copyDockerComposeYml();

            // 用新的docker-compose.yml文件创建容器
            if (active.equalsIgnoreCase("prod")) {
                String cmd = "docker-compose up -d";
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            } else if (active.equalsIgnoreCase("dev")) {
                String cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform; docker-compose up -d");
                Process process = runtime.exec(cmd);
                log.debug("{} return {}", cmd, process.waitFor());
            }

            // 重新加载模型
            reloadModels();

        } catch (Exception e) {
            log.error("e = {}", e);
        }

        isStarted = true;
    }

    @Override
    public void copyDockerComposeYml() throws Exception {
        if (active.equalsIgnoreCase("dev")) {
            String cmd = remoteService.createScpCommand(customConfig.getDockerComposePath(), "/opt/docker/algorithm-manage-platform");
            Process process = runtime.exec(cmd);
            log.debug("{} return {}", cmd, process.waitFor());
        }
    }

    @Override
    public void createDocker(String modelName) throws Exception {
        String cmd = "docker-compose up -d " + modelName;
        if (active.equalsIgnoreCase("dev")) {
            cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform; " + cmd);
            Process process = runtime.exec(cmd);
            log.debug("{} return {}", cmd, process.waitFor());
        } else {
            Process process = runtime.exec(cmd);
            log.debug("{} return {}", cmd, process.waitFor());
        }
    }

    @Override
    public void deleteDocker(String modelName) throws Exception {
        String cmd = "docker-compose rm -sf " + modelName;
        if (active.equalsIgnoreCase("dev")) {
            cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform; " + cmd);
            Process process = runtime.exec(cmd);
            log.debug("{} return {}", cmd, process.waitFor());
        } else if (active.equalsIgnoreCase("prod")) {
            Process process = runtime.exec(cmd);
            log.debug("{} return {}", cmd, process.waitFor());
        }
    }

    @Override
    public void generateDockerComposeYml() throws IOException {

        DockerComposeDTO dockerComposeDTO = new DockerComposeDTO();

        // 配置version
        dockerComposeDTO.setVersion("3");

        // 配置services
        ObjectNode services = yamlObjectMapper.createObjectNode();

        // 添加各个模型的service
        List<ModelEntity> modelEntityList = modelRepository.findAll();
        for (ModelEntity modelEntity : modelEntityList) {
            if (modelEntity.getType() == ModelTypeEnum.MLEAP.getCode()) {
                DockerComposeDTO.Service service = new DockerComposeDTO.Service();
                service.setImage(customConfig.getHarborIp() + "/zrar/mleap-serving:0.9.0-SNAPSHOT");
                service.setNetworks(Arrays.asList("algorithm-bridge"));
                service.setVolumes(Arrays.asList("./models:/models"));
                services.set(modelEntity.getName(), yamlObjectMapper.valueToTree(service));
            } else if (modelEntity.getType() == ModelTypeEnum.TENSORFLOW.getCode()) {
                DockerComposeDTO.Service service = new DockerComposeDTO.Service();
                service.setImage(customConfig.getHarborIp() + "/zrar/tensorflow/serving:latest");
                service.setNetworks(Arrays.asList("algorithm-bridge"));
                service.setVolumes(Arrays.asList("./models/" + modelEntity.getName() + ":/models/" + modelEntity.getName()));
                service.setEnvironment(Arrays.asList("MODEL_NAME=" + modelEntity.getName()));
                services.set(modelEntity.getName(), yamlObjectMapper.valueToTree(service));
            }
        }

        // 添加bridge的service
        DockerComposeDTO.Service bridge = new DockerComposeDTO.Service();
        bridge.setImage(customConfig.getHarborIp() + "/zrar/algorithm-bridge:1.0.0");
        bridge.setNetworks(Arrays.asList("algorithm-bridge"));
        bridge.setPorts(Arrays.asList("8083:8080"));
        services.set("bridge", yamlObjectMapper.valueToTree(bridge));

        // 添加tensorflow-dirtyword-params-transformer的service
        DockerComposeDTO.Service tensorflowDirtywordParamsTransformer = new DockerComposeDTO.Service();
        tensorflowDirtywordParamsTransformer.setImage(customConfig.getHarborIp() + "/zrar/tensorflow-dirtyword-params-transformer:1.0.0");
        tensorflowDirtywordParamsTransformer.setNetworks(Arrays.asList("algorithm-bridge"));
        // 默认是5000端口
        services.set("tensorflow-dirtyword-params-transformer", yamlObjectMapper.valueToTree(tensorflowDirtywordParamsTransformer));

        // 添加mleap-params-transformer的service
        DockerComposeDTO.Service mleapParamsTransformer = new DockerComposeDTO.Service();
        tensorflowDirtywordParamsTransformer.setImage(customConfig.getHarborIp() + "/zrar/mleap-params-transformer:1.0.0");
        tensorflowDirtywordParamsTransformer.setNetworks(Arrays.asList("algorithm-bridge"));
        // 默认是8080端口
        services.set("mleap-params-transformer", yamlObjectMapper.valueToTree(tensorflowDirtywordParamsTransformer));

        dockerComposeDTO.setServices(services);

        // 配置网络
        DockerComposeDTO.Networks networks = new DockerComposeDTO.Networks();
        DockerComposeDTO.Networks.AlgorithmBridge mleapBridge = new DockerComposeDTO.Networks.AlgorithmBridge();
        mleapBridge.setDriver("bridge");
        DockerComposeDTO.Networks.AlgorithmBridge.Ipam ipam = new DockerComposeDTO.Networks.AlgorithmBridge.Ipam();
        ipam.setDriver("default");
        DockerComposeDTO.Networks.AlgorithmBridge.Ipam.Config config = new DockerComposeDTO.Networks.AlgorithmBridge.Ipam.Config();
        config.setSubnet("10.1.1.224/27");
        ipam.setConfig(Arrays.asList(config));
        mleapBridge.setIpam(ipam);
        networks.setAlgorithmBridge(mleapBridge);
        dockerComposeDTO.setNetworks(networks);

        // 将结果写入docker-compose.yml文件中
        yamlObjectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(customConfig.getDockerComposePath()), dockerComposeDTO);
    }

    @Override
    public void reloadModels() {
        List<ModelEntity> modelEntityList = modelRepository.findAll();

        for (ModelEntity modelEntity : modelEntityList) {
            // 这里只有MLeap的模型需要恢复，tensorflow的模型自己就会恢复
            if (modelEntity.getType() == ModelTypeEnum.MLEAP.getCode()) {
                String modelName = modelEntity.getName();
                String modelPath = fileService.getModelOutterPath(modelName);
                File modelFile = new File(modelPath);
                if (modelFile.exists()) {
                    log.debug("开始恢复{}", modelName);
                    mLeapService.online(modelName);
                }
            }
        }
        log.debug("所有模型恢复完毕");
    }
}
