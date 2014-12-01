package com.seigneurin.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import twitter4j.*;
import twitter4j.auth.Authorization;
import twitter4j.auth.AuthorizationFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seigneurin.spark.pojo.Tweet;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

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

    volatile static int counter = 0;

    public static void main(String[] args) throws Exception {
        System.err.println("Entered main...");
        // Twitter4J
        StatusListener listener = new StatusListener() {
            @Override
	    public void onStatus(Status status) {
                counter++;
                System.out.println(status.getUser().getName() + " : " + status.getText());
            }
            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}
	    @Override public void onStallWarning(StallWarning warning) {}
	    @Override public void onScrubGeo(long i,long f) {}
            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(listener);
        // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
        twitterStream.sample();

        while (counter < 10) {
                Thread.sleep(25);
        }
        twitterStream.shutdown(); //??

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
                .setAppName("Tweets #CyberMonday")
                .set("spark.serializer", KryoSerializer.class.getName())
                .set("es.nodes", "elasticsearch-aws-3.weave.local:9200")
                .set("es.index.auto.create", "true");
        JavaStreamingContext sc = new JavaStreamingContext(sparkConf, new Duration(5000));

        String[] filters = { };
        TwitterUtils.createStream(sc, twitterAuth, filters)
                //.map(s -> new Tweet(s.getUser().getName(), s.getText(), s.getCreatedAt(), detectLanguage(s.getText())))
                //.map(t -> mapper.writeValueAsString(t))
                .foreachRDD(new Function2<JavaPairRDD<String, Integer>, Time, Void>() {
                    // https://issues.apache.org/jira/browse/SPARK-4560
                    // tweets.foreach(t -> System.out.println(t));
                    @Override
                    public Void call(JavaPairRDD<String, Integer> rdd, Time time) throws IOException {
                      String counts = "Counts at time " + time + " " + rdd.collect();
                      System.out.println(counts);
                      System.out.println("Appending to " + outputFile.getAbsolutePath());
                      Files.append(counts + "\n", outputFile, Charset.defaultCharset());
                      return null;
                    }

                    //System.out.println("Saving tweets - count:");
                    //System.out.println(tweets.count());
                    ////tweets.collect().stream().forEach(t -> System.out.println(t));
                    ////JavaEsSpark.saveJsonToEs(tweets, "spark/tweets");
                    //return null;
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
