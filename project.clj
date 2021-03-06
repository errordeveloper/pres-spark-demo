(defproject pres-spark-demo "0.0.1"
    :dependencies [
        [org.apache.spark/spark-core_2.10 "1.1.0"]
        [org.apache.spark/spark-streaming-twitter_2.10 "1.1.0"]
        [org.apache.hadoop/hadoop-client "2.5.0"]
        [org.elasticsearch/elasticsearch-spark_2.10 "2.1.0.Beta3"]
        [com.fasterxml.jackson.core/jackson-databind "2.4.3"]
	[junit "4.11"]
        [org.jodd/jodd-core "3.6.2"]
        ;;[org.apache.commons/commons-io "1.3.2"]

        ;;[org.clojure/clojure "{{clojure-version}}"]
    ]
  :java-source-paths ["src/main/java"]
  :resource-paths ["src/main/resources"]
  :profiles { :uberjar {:aot :all} }
  :local-repo "local-m2"
  :main com.seigneurin.spark.IndexTweets)
