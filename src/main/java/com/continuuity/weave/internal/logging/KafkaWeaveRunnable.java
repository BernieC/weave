/**
 * Copyright 2012-2013 Continuuity,Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.continuuity.weave.internal.logging;

import com.continuuity.weave.api.Command;
import com.continuuity.weave.api.WeaveContext;
import com.continuuity.weave.api.WeaveRunnable;
import com.continuuity.weave.api.WeaveRunnableSpecification;
import com.continuuity.weave.internal.utils.Networks;
import com.continuuity.weave.internal.yarn.EnvKeys;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;

/**
 * A {@link WeaveRunnable} for managing Kafka server.
 */
public final class KafkaWeaveRunnable implements WeaveRunnable {

  private final String kafkaDir;
  private Object server;

  public KafkaWeaveRunnable(String kafkaDir) {
    this.kafkaDir = kafkaDir;
  }

  @Override
  public WeaveRunnableSpecification configure() {
    return WeaveRunnableSpecification.Builder.with()
      .setName("kafka")
      .withConfigs(ImmutableMap.of("kafkaDir", kafkaDir))
      .build();
  }

  @Override
  public void initialize(WeaveContext context) {
    Map<String,String> args = context.getSpecification().getConfigs();
    String zkConnectStr = System.getenv(EnvKeys.WEAVE_LOG_KAFKA_ZK);

    try {
      ClassLoader classLoader = getClassLoader(new File(args.get("kafkaDir")));

      Class<?> configClass = classLoader.loadClass("kafka.server.KafkaConfig");
      Object config = configClass.getConstructor(Properties.class)
                                 .newInstance(generateKafkaConfig(zkConnectStr));

      Class<?> serverClass = classLoader.loadClass("kafka.server.KafkaServerStartable");
      server = serverClass.getConstructor(configClass).newInstance(config);

      serverClass.getMethod("startup").invoke(server);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void handleCommand(Command command) throws Exception {
  }

  @Override
  public void stop() {
    try {
      server.getClass().getMethod("shutdown").invoke(server);
      server.getClass().getMethod("awaitShutdown").invoke(server);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void run() {
    try {
      server.getClass().getMethod("awaitShutdown").invoke(server);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private ClassLoader getClassLoader(File kafkaDir) throws MalformedURLException {
    String[] cp = new String[]{
      "project/boot/scala-2.8.0/lib/scala-compiler.jar",
      "project/boot/scala-2.8.0/lib/scala-library.jar",
      "core/target/scala_2.8.0/kafka-0.7.2.jar",
      "perf/target/scala_2.8.0/kafka-perf-0.7.2.jar",
      "core/lib_managed/scala_2.8.0/compile/jopt-simple-3.2.jar",
      "core/lib_managed/scala_2.8.0/compile/log4j-over-slf4j-1.6.4.jar",
      "core/lib_managed/scala_2.8.0/compile/snappy-java-1.0.4.1.jar",
      "core/lib_managed/scala_2.8.0/compile/zkclient-0.1.jar",
      "core/lib_managed/scala_2.8.0/compile/zookeeper-3.3.4.jar"};
    URL[] urls = new URL[cp.length];

    for (int i = 0; i < cp.length; i++) {
      urls[i] = new File(kafkaDir, cp[i]).toURI().toURL();
    }
    return new URLClassLoader(urls);
  }

  private Properties generateKafkaConfig(String zkConnectStr) {
    int port = Networks.getRandomPort();
    Preconditions.checkState(port > 0, "Failed to get random port.");

    Properties prop = new Properties();
    prop.setProperty("log.dir", new File("kafka-logs").getAbsolutePath());
    prop.setProperty("zk.connect", zkConnectStr);
    prop.setProperty("num.threads", "8");
    prop.setProperty("port", Integer.toString(port));
    prop.setProperty("log.flush.interval", "10000");
    prop.setProperty("max.socket.request.bytes", "104857600");
    prop.setProperty("log.cleanup.interval.mins", "1");
    prop.setProperty("log.default.flush.scheduler.interval.ms", "1000");
    prop.setProperty("zk.connectiontimeout.ms", "1000000");
    prop.setProperty("socket.receive.buffer", "1048576");
    prop.setProperty("enable.zookeeper", "true");
    prop.setProperty("log.retention.hours", "168");
    prop.setProperty("brokerid", "0");
    prop.setProperty("socket.send.buffer", "1048576");
    prop.setProperty("num.partitions", "1");
    prop.setProperty("log.file.size", "536870912");
    prop.setProperty("log.default.flush.interval.ms", "1000");
    return prop;
  }
}
