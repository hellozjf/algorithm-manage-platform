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
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.List;

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
                new String[]{"这是一个伸手不见五指的黑夜。我叫孙悟空，我爱北京，我爱Python和C++。", "我不喜欢日本和服。", "雷猴回归人间。",
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

    @Test
    public void loadModel() throws Exception {
        try (Graph graph = new Graph()) {
            //导入图
            Resource resource = new ClassPathResource("static/tensorflow/modelPb/model.pb");
            byte[] graphBytes = IOUtils.toByteArray(resource.getInputStream());
            graph.importGraphDef(graphBytes);

            //根据图建立Session
            try (Session session = new Session(graph)) {
                //相当于TensorFlow Python中的sess.run(z, feed_dict = {'x': 10.0})
                float z = session.runner()
                        .feed("x", Tensor.create(10.0f))
                        .fetch("z").run().get(0).floatValue();
                System.out.println("z = " + z);
            }
        }
    }

    @Test
    public void loadHalfPlusTwo() throws Exception {
        //导入图
        ClassPathResource resource = new ClassPathResource("static/tensorflow/modelPb/saved_model_half_plus_two_cpu/00000123");
        SavedModelBundle savedModelBundle = SavedModelBundle.load(resource.getFile().getAbsolutePath(), "serve");
        float y = savedModelBundle.session().runner()
                .feed("x", Tensor.create(10.0f))
                .fetch("y").run().get(0).floatValue();
        System.out.println("y = " + y);
    }

    @Test
    public void loadDirtyWord() throws Exception {
        //导入图
        Resource resource = new ClassPathResource("static/tensorflow/modelPb/dirtyWord/1562028395");
        SavedModelBundle savedModelBundle = SavedModelBundle.load(resource.getFile().getAbsolutePath(), "serve");
        long[] inputIds = new long[] {101,4003,102,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        long[] inputMask = new long[] {1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        long[] segmentIds = new long[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        long[] shape = new long[] {1, 128};
        Tensor y = savedModelBundle.session().runner()
                .feed("input_ids", Tensor.create(shape, LongBuffer.wrap(inputIds)))
                .feed("input_mask", Tensor.create(shape, LongBuffer.wrap(inputMask)))
                .feed("segment_ids", Tensor.create(shape, LongBuffer.wrap(segmentIds)))
                .feed("unique_ids", Tensor.create(0L))
                .fetch("loss/Softmax").run().get(0);
        float[] result = new float[] {0, 0};
        FloatBuffer floatBuffer = FloatBuffer.wrap(result);
        y.writeTo(floatBuffer);
        log.debug("result[0] = {}, result[1] = {}", result[0], result[1]);
    }

    @Test
    public void loadQa() throws Exception {
        //导入图
        Resource resource = new ClassPathResource("static/tensorflow/modelPb/qa/1564561998");
        SavedModelBundle savedModelBundle = SavedModelBundle.load(resource.getFile().getAbsolutePath(), "serve");
        long[] answer = new long[] {2,5,1717,1905,3,1030,192,269,78,976,530,6,192,530,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        long[] question = new long[] {2,88,25,718,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        long[] answerLen = new long[] {14};
        long[] questionLen = new long[] {4};
        long[] shape1 = new long[] {1, 120};
        long[] shape2 = new long[] {1, 1};
        Tensor y = savedModelBundle.session().runner()
                .feed("answer", Tensor.create(shape1, LongBuffer.wrap(answer)))
                .feed("question", Tensor.create(shape1, LongBuffer.wrap(question)))
                .feed("answer_len", Tensor.create(shape2, LongBuffer.wrap(answerLen)))
                .feed("question_len", Tensor.create(shape2, LongBuffer.wrap(questionLen)))
                .fetch("bilstm/attentive-pooling/Squeeze").run().get(0);
        double[] result = new double[256];
        DoubleBuffer doubleBuffer = DoubleBuffer.wrap(result);
        y.writeTo(doubleBuffer);
        log.debug("result = {}", Arrays.toString(result));
    }

}
