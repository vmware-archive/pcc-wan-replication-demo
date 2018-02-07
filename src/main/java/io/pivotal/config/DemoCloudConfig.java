package io.pivotal.config;

import io.pivotal.domain.Customer;
import io.pivotal.spring.cloud.service.gemfire.GemfireServiceConnectorConfig;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.ServiceConnectorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;

@Configuration
@Profile("cloud")
public class DemoCloudConfig extends AbstractCloudConfig {
	
	@Autowired 
	ClientCache clientCache;
	
	public ServiceConnectorConfig createGemfireConnectorConfig() {

        GemfireServiceConnectorConfig gemfireConfig = new GemfireServiceConnectorConfig();
        gemfireConfig.setPoolSubscriptionEnabled(true);
        gemfireConfig.setPdxSerializer(new ReflectionBasedAutoSerializer(".*"));
        gemfireConfig.setPdxReadSerialized(false);

        return gemfireConfig;
    }
    
	@Bean(name = "gemfireCache")
    public ClientCache getGemfireClientCache() throws Exception {		
		
		Cloud cloud = new CloudFactory().getCloud();
		ClientCache clientCache = cloud.getSingletonServiceConnector(ClientCache.class,  createGemfireConnectorConfig());

        return clientCache;
    }

	@Bean(name = "customer")
	public ClientRegionFactoryBean<String, Customer> customerRegion() {
		ClientRegionFactoryBean<String, Customer> customerRegionFactory = new ClientRegionFactoryBean<>();
		customerRegionFactory.setCache(clientCache);
		customerRegionFactory.setShortcut(ClientRegionShortcut.PROXY);
		customerRegionFactory.setName("customer");

		return customerRegionFactory;
	}

}
