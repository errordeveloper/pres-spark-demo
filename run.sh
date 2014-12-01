git fetch myfork
git reset --hard myfork/master
docker run -v `pwd`:/io errordeveloper/lein uberjar
sudo weave run --with-dns 10.10.1.50/24 --entrypoint spark-submit -h spark-submit-aws-1.weave.local -v ~/pres-spark-demo/:/io errordeveloper/weave-spark-shell-minimal --master spark://spark-master-gce.weave.local:7077 --class com.seigneurin.spark.IndexTweets /io/target/pres-spark-demo-0.0.1-standalone.jar spark://spark-master-gce.weave.local:7077
