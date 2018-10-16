pid=`ps -ef | grep java | grep -v grep | awk '{ print $2}'`
if [ -n "$pid" ]
then
        kill -9  $pid
fi
nohup java -jar ../jars/easyHbase-1.0dock-SNAPSHOT.jar  > /dev/null 2>&1 &