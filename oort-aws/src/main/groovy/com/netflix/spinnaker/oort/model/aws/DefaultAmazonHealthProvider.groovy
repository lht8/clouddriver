/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.oort.model.aws

import com.amazonaws.services.ec2.model.Instance
import com.netflix.spinnaker.oort.data.aws.Keys
import com.netflix.spinnaker.oort.data.aws.cachers.InstanceCachingAgent
import com.netflix.spinnaker.oort.model.CacheService
import com.netflix.spinnaker.oort.model.Health
import com.netflix.spinnaker.oort.model.HealthProvider
import com.netflix.spinnaker.oort.model.HealthState
import com.netflix.spinnaker.oort.model.ServerGroup
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@CompileStatic
class DefaultAmazonHealthProvider implements HealthProvider {
  public static final String HEALTH_TYPE = "Amazon"

  @Autowired
  CacheService cacheService

  @Override
  Health getHealth(String account, ServerGroup serverGroup, String instanceId) {
    def cacheKey = Keys.getInstanceKey(instanceId, serverGroup.region)
    def instance = cacheService.retrieve(cacheKey, Instance)
    if (!instance) {
      return new AwsInstanceHealth(type: HEALTH_TYPE, instanceId: instanceId, state: HealthState.Unknown)
    }
    def state = InstanceCachingAgent.InstanceStateValue.fromInstanceState(instance.state)
    if (state != InstanceCachingAgent.InstanceStateValue.Running) {
      return new AwsInstanceHealth(type: HEALTH_TYPE, instanceId: instanceId, state: HealthState.Down)
    }
    //AWS instance lifecycle doesn't confidently tell us the application is Up, only that it is not Down:
    return new AwsInstanceHealth(type: HEALTH_TYPE, instanceId: instanceId, state: HealthState.Unknown)
  }
}
