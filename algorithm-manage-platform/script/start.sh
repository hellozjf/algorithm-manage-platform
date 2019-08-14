source config.txt
java -jar algorithm-manage-platform-$version.jar --spring.profiles.active=prod --custom.platform-path=`pwd` --custom.harbor-ip=$harbor --server.port=$port --bridge-port=$bridgePort &
