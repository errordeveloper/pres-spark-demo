#git fetch myfork
#git reset --hard myfork/master
docker run -v `pwd`:/io errordeveloper/lein uberjar \
  && sudo weave run --with-dns 10.10.1.55/24 \
    --entrypoint spark-submit -h spark-submit-aws-5.weave.local \
    -e SPARK_PRINT_LAUNCH_COMMAND=1 \
    -v ~/pres-spark-demo/:/io errordeveloper/weave-spark-shell-minimal \
    --master spark://spark-master-gce.weave.local:7077 \
    --class com.seigneurin.spark.IndexTweets \
    --driver-java-options "-Dtwitter4j.loggerFactory=twitter4j.Log4JLoggerFactory -Dtwitter4j.debug=true -Dtwitter4j.http.prettyDebug=true" \
    /io/target/pres-spark-demo-0.0.1-standalone.jar \
    spark://spark-master-gce.weave.local:7077
