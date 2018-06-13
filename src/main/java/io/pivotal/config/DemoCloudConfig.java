package io.pivotal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnableLogging;
import org.springframework.geode.config.annotation.EnableDurableClient;
import org.springframework.geode.config.annotation.UseMemberName;

@Configuration
@EnableDurableClient(id = "cache-demo")
@EnableEntityDefinedRegions(basePackages = {"io.pivotal.domain"})
@EnableLogging(logLevel = "info")
@UseMemberName("PivotalCloudCacheWANApplication")
@SuppressWarnings("unused")
public class DemoCloudConfig {
}