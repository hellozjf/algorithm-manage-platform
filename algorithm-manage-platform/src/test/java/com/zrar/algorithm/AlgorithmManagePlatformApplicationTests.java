package com.zrar.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class AlgorithmManagePlatformApplicationTests {

//    @Test
    public void contextLoads() {
        try (Graph graph = new Graph()) {
            //导入图
            Resource resource = new ClassPathResource("saved_model.pb");
            byte[] graphBytes = IOUtils.toByteArray(resource.getInputStream());
            graph.importGraphDef(graphBytes);

            //根据图建立Session
            try (Session session = new Session(graph)) {
                //相当于TensorFlow Python中的sess.run(z, feed_dict = {'x': 10.0})
                float z = session.runner()
                        .feed("input_ids", Tensor.create(10.0f))
                        .fetch("z").run().get(0).floatValue();
                System.out.println("z = " + z);
            }
        } catch (Exception e) {
            log.error("e = {}", e);
        }
    }

}
