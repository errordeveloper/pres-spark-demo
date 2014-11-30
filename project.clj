(defproject leininge-spark-demo "0.0.1"
    :dependencies [
        [org.apache.spark/spark-core_2.10 "1.1.0"]
        [org.apache.spark/spark-streaming-twitter_2.10 "1.1.0"]
        [org.apache.hadoop/hadoop-client "2.5.0"]
        [org.elasticsearch/elasticsearch-spark_2.10 "2.1.0.Beta3"]
        [com.fasterxml.jackson.core/jackson-databind "2.4.3"]

        [org.clojure/clojure "{{clojure-version}}"]
    ]
  :main com.holdenkarau.esspark.ReIndexTweets)