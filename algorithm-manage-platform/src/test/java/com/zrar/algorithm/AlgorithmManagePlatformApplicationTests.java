package com.zrar.algorithm;

import com.huaban.analysis.jieba.JiebaSegmenter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring测试单元测试的时候，不要启动CommandLineRunner
 * https://blog.csdn.net/tengyuanjack/article/details/78438184
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
@ActiveProfiles("unittest")
public class AlgorithmManagePlatformApplicationTests {

    @Test
    public void testDemo() {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        String[] sentences =
                new String[] {"这是一个伸手不见五指的黑夜。我叫孙悟空，我爱北京，我爱Python和C++。", "我不喜欢日本和服。", "雷猴回归人间。",
                        "工信处女干事每月经过下属科室都要亲口交代24口交换机等技术性器件的安装工作", "结果婚的和尚未结过婚的"};
        for (String sentence : sentences) {
            System.out.println(segmenter.process(sentence, JiebaSegmenter.SegMode.INDEX).toString());
        }
    }

    /**
     * 切词
     */
    @Test
    public void lcut() {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        String str = "请杭州未来客服代表电波收费万说钱希望工商我查一下交钱消费预收收款预收款300昨天离开锁定以后随意扣91沟通现在告诉早餐费贵姓姓张弓长张全名提供一下我想装王字旁七块钱暂时女士电脑消费身份身份证";
        List<String> wordList = segmenter.sentenceProcess(str);
        log.debug("wordList = {}", wordList);
    }

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
