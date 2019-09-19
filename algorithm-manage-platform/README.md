# 使用方式

## 目录结构

```
.
├── clear.sh
├── config.txt
├── jar
│   ├── algorithm-manage-platform-1.0.0.jar
│   ├── algorithm-manage-platform-1.0.1.jar
│   ├── algorithm-manage-platform-1.0.2.jar
│   └── algorithm-manage-platform-1.0.3.jar
├── start.sh
└── stop.sh
```



## 启动

首先编辑config.txt，配置正确的harbor地址和端口，以及应用的版本号

```
harbor=192.168.2.150
port=8081
bridgePort=8084
version=1.0.3
```

然后运行启动脚本

```
./start.sh
```

## 停止

运行停止脚本

```
./stop.sh
```

## 清除所有数据

```
./clear.sh
```



# postman测试tensorflow

```
http://192.168.56.102:8084/tensorflow/qa/v1/models/qa/metadata
```



# 待处理的问题

2. mleap更改为spark mlib
3. 邵成杰的模型进行统一处理，选择词库、选择模型种类、选择是否去停词、输入位数
4. 康一帅的模型，需要做后处理（矩阵求相似度），一次调用两个模型

# 版本信息

## 1.0.9

问答匹配模型增加原问题，优化界面

## 1.0.8

更新问答匹配模型

## 1.0.7

添加智能咨询模型

## 1.0.5

添加三分类模型（判断是城管、社保还是综合），城管模型，社保模型，综合模型

## 1.0.4

将tensorflow问答匹配模型的前处理步骤，从python转化为java，但是感觉效果并不好。java的hanlp和python的解霸切出来的词并不一样，而且速度也只是从几十毫秒缩短到几毫秒而已

## 1.0.3

tensorflow增加问答匹配模型，同时调整前端

## 1.0.2

tensorflow获取参数的函数使用java实现

## 1.0.1

添加tensorflow的问题过滤模型（也称为有效无效模型）

## 1.0.0

移植mleap的税务模型、情感分析模型、语音通话模型、纳税人模型、问答模型
添加tensorflow的脏话模型、情感分析模型