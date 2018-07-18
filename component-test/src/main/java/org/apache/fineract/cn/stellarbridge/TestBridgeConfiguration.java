/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.stellarbridge;

import org.apache.fineract.cn.anubis.test.v1.TenantApplicationSecurityEnvironmentTestRule;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.stellarbridge.api.v1.client.StellarBridgeManager;
import org.apache.fineract.cn.stellarbridge.api.v1.domain.BridgeConfiguration;
import org.apache.fineract.cn.stellarbridge.api.v1.events.EventConstants;
import org.apache.fineract.cn.stellarbridge.service.internal.config.StellarBridgeConfiguration;
import org.apache.fineract.cn.test.fixture.TenantDataStoreContextTestRule;
import org.apache.fineract.cn.test.listener.EnableEventRecording;
import org.apache.fineract.cn.test.listener.EventRecorder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {"stellarBridge.user=homer", "stellarBridge.horizonAddress=https://horizon-testnet.stellar.org"})
public class TestBridgeConfiguration extends SuiteTestEnvironment {
  private static final String LOGGER_NAME = "test-logger";
  private static final String TEST_USER = "homer";


  @Configuration
  @EnableEventRecording
  @EnableFeignClients(basePackages = {"org.apache.fineract.cn.stellarbridge.api.v1.client"})
  @RibbonClient(name = APP_NAME)
  @Import({StellarBridgeConfiguration.class})
  @ComponentScan("org.apache.fineract.cn.stellarbridge.listener")
  public static class TestConfiguration {
    public TestConfiguration() {
      super();
    }

    @Bean(name = LOGGER_NAME)
    public Logger logger() {
      return LoggerFactory.getLogger(LOGGER_NAME);
    }
  }

  @ClassRule
  public final static TenantDataStoreContextTestRule tenantDataStoreContext = TenantDataStoreContextTestRule.forRandomTenantName(cassandraInitializer, mariaDBInitializer);

  @Rule
  public final TenantApplicationSecurityEnvironmentTestRule tenantApplicationSecurityEnvironment
          = new TenantApplicationSecurityEnvironmentTestRule(testEnvironment, this::waitForInitialize);

  private AutoUserContext userContext;

  @Autowired
  private StellarBridgeManager testSubject;

  @Autowired
  private EventRecorder eventRecorder;

  @SuppressWarnings("WeakerAccess")
  @Autowired
  @Qualifier(LOGGER_NAME)
  Logger logger;

  public TestBridgeConfiguration() {
    super();
  }

  @Before
  public void prepTest() {
    userContext = tenantApplicationSecurityEnvironment.createAutoUserContext(TestBridgeConfiguration.TEST_USER);
  }

  @After
  public void cleanTest() {
    userContext.close();
    eventRecorder.clear();
  }

  private boolean waitForInitialize() {
    try {
      return this.eventRecorder.wait(EventConstants.INITIALIZE, APP_VERSION);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void shouldSetConfiguration() throws InterruptedException {
    logger.info("Running test shouldSetConfiguration.");
    final BridgeConfiguration bridgeConfiguration =
        new BridgeConfiguration(
            "blah",
            "blah",
            "blah",
            "blah",
            "blah");
    this.testSubject.setBridgeConfiguration(bridgeConfiguration);

    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CONFIG, null));

    final BridgeConfiguration currentConfiguration = this.testSubject.getBridgeConfiguration();
    Assert.assertEquals(bridgeConfiguration, currentConfiguration);
  }
}
