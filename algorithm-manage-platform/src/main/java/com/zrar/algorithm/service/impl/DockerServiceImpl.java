package com.zrar.algorithm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zrar.algorithm.config.CustomConfig;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.domain.ModelEntity;
import com.zrar.algorithm.dto.DockerComposeDTO;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.repository.ModelRepository;
import com.zrar.algorithm.service.DockerService;
import com.zrar.algorithm.service.FileService;
import com.zrar.algorithm.service.MLeapService;
import com.zrar.algorithm.service.RemoteService;
import com.zrar.algorithm.util.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;

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

            // 如果是tensorflow模型，还需要解压
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName().split("\\.")[0];
                    log.debug("name = {}", name);
                    if (!modelRepository.findByName(name).isPresent()) {
                        log.error("找不到文件{}对应的数据库记录，这可能是个脏文件", file.getName());
                        continue;
                    }
                    ModelEntity modelEntity = modelRepository.findByName(name).get();
                    if (modelEntity.getType() == ModelTypeEnum.TENSORFLOW.getCode()) {
                        unpackModel(modelEntity.getName());
                    }
                }
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
    public void unpackModel(String name) throws IOException, InterruptedException {
        if (active.equalsIgnoreCase("dev")) {
            // 开发环境，进入远程服务器，解压模型
            String cmd = remoteService.createExecCommand("cd /opt/docker/algorithm-manage-platform/models/; unzip -o " + name + ".zip -d " + name);
            Process process = runtime.exec(cmd);
            log.debug("{} return {}", cmd, process.waitFor());
        } else {
            // 生产环境，进入宿主机模型目录，解压模型
            String cmd = "unzip -o " + customConfig.getModelOuterPath() + "/" + name + ".zip -d " + customConfig.getModelOuterPath() + "/" + name;
            Process process = runtime.exec(cmd);
            log.debug("{} return {}", cmd, process.waitFor());
        }
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
    public void restartDocker(String modelName) {
        try {
            log.debug("关闭容器");
            deleteDocker(modelName);
            log.debug("启动容器");
            createDocker(modelName);

            ModelEntity modelEntity = modelRepository.findByName(modelName).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            if (modelEntity.getType() == ModelTypeEnum.MLEAP.getCode()) {
                String modelPath = fileService.getModelOutterPath(modelName);
                File modelFile = new File(modelPath);
                if (modelFile.exists()) {
                    log.debug("开始恢复{}", modelName);
                    mLeapService.online(modelName);
                }
            }
        } catch (Exception e) {
            log.error("e = {}", e);
        }
    }

    @Override
    public void createDocker(String modelName) throws Exception {
        String cmd = "docker-compose up -d " + modelName;
        execCommand(cmd);
    }

    @Override
    public void deleteDocker(String modelName) throws Exception {
        String cmd = "docker-compose rm -sf " + modelName;
        execCommand(cmd);
    }

    private void execCommand(String cmd) throws IOException, InterruptedException {
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
                // 如果本地有tensorflow-serving，那么就用这个优化过的镜像，没有还是使用官方镜像
                if (haveOptimize()) {
                    service.setImage(customConfig.getHarborIp() + "/zrar/tensorflow-serving:1.14.0");
                } else {
                    service.setImage(customConfig.getHarborIp() + "/zrar/tensorflow/serving:1.14.0");
                }
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
        bridge.setPorts(Arrays.asList(customConfig.getBridgePort() + ":8080"));
        services.set("bridge", yamlObjectMapper.valueToTree(bridge));

        // 添加tensorflow-params-transformer的service
        DockerComposeDTO.Service tensorflowParamsTransformer = new DockerComposeDTO.Service();
        // TODO 每添加一个模型，需要修改tensorflow-params-transformer的版本号
        tensorflowParamsTransformer.setImage(customConfig.getHarborIp() + "/zrar/tensorflow-params-transformer:1.0.11");
        tensorflowParamsTransformer.setNetworks(Arrays.asList("algorithm-bridge"));
        // 默认是5000端口
        services.set("tensorflow-params-transformer", yamlObjectMapper.valueToTree(tensorflowParamsTransformer));

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

    /**
     * 判断机器上是否有对应的cpu优化版tensorflow-serving
     * 通过docker images | grep tensorflow-serving判断
     *
     * @return
     */
    private boolean haveOptimize() {
        try {
            if (active.equalsIgnoreCase("dev")) {
                String cmd = remoteService.createExecCommand("docker images | grep tensorflow-serving");
                Process process = runtime.exec(cmd);
                String result = ProcessUtils.getInputStreamString(process);
                log.debug("result = {}", result);
                if (StringUtils.isEmpty(result)) {
                    return false;
                } else {
                    return true;
                }
            } else {
                String cmd = "docker images | grep tensorflow-serving";
                Process process = runtime.exec(new String[] {"sh", "-c", cmd});
                String result = ProcessUtils.getInputStreamString(process);
                log.debug("result = {}", result);
                if (StringUtils.isEmpty(result)) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("e = {}", e);
            return false;
        }
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
