package com.zrar.algorithm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * @author Jingfeng Zhou
 */
@Data
public class DockerComposeDTO {
    private String version;
    private JsonNode services;
    private Networks networks;

    @Data
    public static class Service {
        private String image;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<String> ports;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<String> volumes;
        private List<String> networks;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<String> environment;
    }

    @Data
    public static class Networks {
        @JsonProperty("algorithm-bridge")
        private AlgorithmBridge algorithmBridge;

        @Data
        public static class AlgorithmBridge {
            private String driver;
            private Ipam ipam;

            @Data
            public static class Ipam {
                private String driver;
                private List<Config> config;

                @Data
                public static class Config {
                    private String subnet;
                }
            }
        }
    }
}
