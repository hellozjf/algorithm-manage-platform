# 使用方式

## 目录结构

```
.
├── algorithm-manage-platform-1.0.2.jar
├── clear.sh
├── config.txt
├── README.md
├── start.sh
└── stop.sh

```



## 启动

首先编辑config.txt，配置正确的harbor地址和端口，以及应用的版本号

```
harbor=192.168.2.150
port=8081
version=1.0.2

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



# 版本信息

## 1.0.3

tensorflow增加问答匹配模型，同时调整前端

## 1.0.2

tensorflow获取参数的函数使用java实现

## 1.0.1

添加tensorflow的问题过滤模型（也称为有效无效模型）

## 1.0.0

移植mleap的税务模型、情感分析模型、语音通话模型、纳税人模型、问答模型
添加tensorflow的脏话模型、情感分析模型