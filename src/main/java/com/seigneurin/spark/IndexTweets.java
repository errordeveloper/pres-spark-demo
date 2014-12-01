package com.seigneurin.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import twitter4j.auth.Authorization;
import twitter4j.auth.AuthorizationFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seigneurin.spark.pojo.Tweet;

import jodd.util.ClassLoaderUtil;
import java.util.List;
import java.util.Vector;
import jodd.io.FileUtil;
import jodd.io.findfile.ClassScanner;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import org.apache.commons.io.IOUtils;
import jodd.util.StringPool;
import jodd.util.StringUtil;
import jodd.core.JoddCore;


public class IndexTweets {

    public static void main(String[] args) throws Exception {
        System.err.println("Entered main...");
        // Twitter4J
        // IMPORTANT: ajuster vos clés d'API dans twitter4J.properties
        Configuration twitterConf = ConfigurationContext.getInstance();
        Authorization twitterAuth = AuthorizationFactory.getInstance(twitterConf);

        // Jackson
        ObjectMapper mapper = new ObjectMapper();

        // Language Detection
        List<String> profiles = new Vector<String>();
        ClassScanner scanner = new ClassScanner() {
            @Override
            protected void onEntry(EntryData entryData) throws Exception {
                //String encoding = JoddCore.encoding;
                if (StringUtil.startsWithIgnoreCase(entryData.getName(), "/profiles/") 
                    && !StringUtil.endsWithIgnoreCase(entryData.getName(), "/profiles/")) {
                    //encoding = StringPool.ISO_8859_1;
                    System.err.println("Found profile: " + entryData.getName());
                    profiles.add(
                        FileUtil.readUTFString(
                            entryData.openInputStream()
                        )
                    );
                }
            }
        };
        scanner.setIncludeResources(true);
        scanner.setIgnoreException(true);
        //scanner.setExcludeAllEntries(true);
        //scanner.setIncludedEntries("/profiles/*");
        //scanner.scanDefaultClasspath();

        scanner.scan(
            FileUtil.toContainerFile(
              ClassLoaderUtil.getResourceUrl("/profiles/")
            )
        );
        System.err.println("Loading profiles...");
        DetectorFactory.loadProfile(profiles);

        // Spark
        SparkConf sparkConf = new SparkConf()
                .setAppName("Tweets Android")
                .setMaster("spark://spark-master-gce.weave.local:7077")
                .set("spark.serializer", KryoSerializer.class.getName())
                .set("es.nodes", "elasticsearch-aws-3.weave.local:9200")
                .set("es.index.auto.create", "true");
        JavaStreamingContext sc = new JavaStreamingContext(sparkConf, new Duration(5000));

        String[] filters = { "#Android" };
        TwitterUtils.createStream(sc, twitterAuth, filters)
                .map(s -> new Tweet(s.getUser().getName(), s.getText(), s.getCreatedAt(), detectLanguage(s.getText())))
                .map(t -> mapper.writeValueAsString(t))
                .foreachRDD(tweets -> {
                    // https://issues.apache.org/jira/browse/SPARK-4560
                    // tweets.foreach(t -> System.out.println(t));

                    tweets.collect().stream().forEach(t -> System.out.println(t));
                    JavaEsSpark.saveJsonToEs(tweets, "spark/tweets");
                    System.out.println("Saving tweets - count:"
                    System.out.println(tweets.count());
                    return null;
                });

        sc.start();
        sc.awaitTermination();
    }

    private static String detectLanguage(String text) throws Exception {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.detect();
    }
}
