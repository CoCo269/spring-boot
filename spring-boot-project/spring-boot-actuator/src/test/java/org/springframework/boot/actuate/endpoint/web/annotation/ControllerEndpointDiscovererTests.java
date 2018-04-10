/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ControllerEndpointDiscoverer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ControllerEndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void getEndpointsWhenNoEndpointBeansShouldReturnEmptyCollection() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class).run(
				assertDiscoverer((discoverer) ->
						assertThat(discoverer.getEndpoints()).isEmpty()));
	}

	@Test
	public void getEndpointsShouldIncludeControllerEndpoints() {
		this.contextRunner.withUserConfiguration(TestControllerEndpoint.class)
				.run(assertDiscoverer((discoverer) -> {
					Collection<ExposableControllerEndpoint> endpoints = discoverer.getEndpoints();
					assertThat(endpoints).hasSize(1);
					ExposableControllerEndpoint endpoint = endpoints.iterator().next();
					assertThat(endpoint.getId()).isEqualTo("testcontroller");
					assertThat(endpoint.getController())
							.isInstanceOf(TestControllerEndpoint.class);
					assertThat(endpoint).isInstanceOf(DiscoveredEndpoint.class);
				}));
	}

	@Test
	public void getEndpointsShouldIncludeRestControllerEndpoints() {
		this.contextRunner.withUserConfiguration(TestRestControllerEndpoint.class)
				.run(assertDiscoverer((discoverer) -> {
					Collection<ExposableControllerEndpoint> endpoints = discoverer.getEndpoints();
					assertThat(endpoints).hasSize(1);
					ExposableControllerEndpoint endpoint = endpoints.iterator().next();
					assertThat(endpoint.getId()).isEqualTo("testrestcontroller");
					assertThat(endpoint.getController())
							.isInstanceOf(TestRestControllerEndpoint.class);
				}));
	}

	@Test
	public void getEndpointsShouldNotDiscoverRegularEndpoints() {
		this.contextRunner.withUserConfiguration(WithRegularEndpointConfiguration.class)
				.run(assertDiscoverer((discoverer) -> {
					Collection<ExposableControllerEndpoint> endpoints = discoverer.getEndpoints();
					List<String> ids = endpoints.stream().map(ExposableEndpoint::getId)
							.collect(Collectors.toList());
					assertThat(ids).containsOnly("testcontroller", "testrestcontroller");
				}));
	}

	@Test
	public void getEndpointWhenEndpointHasOperationsShouldThrowException() {
		this.contextRunner.withUserConfiguration(TestControllerWithOperation.class)
				.run(assertDiscoverer((discoverer) -> {
					this.thrown.expect(IllegalStateException.class);
					this.thrown.expectMessage("ControllerEndpoints must not declare operations");
					discoverer.getEndpoints();
				}));
	}

	private ContextConsumer<AssertableApplicationContext> assertDiscoverer(
			Consumer<ControllerEndpointDiscoverer> consumer) {
		return (context) -> {
			ControllerEndpointDiscoverer discoverer = new ControllerEndpointDiscoverer(
					context, PathMapper.useEndpointId(), Collections.emptyList());
			consumer.accept(discoverer);
		};
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	@Import({ TestEndpoint.class, TestControllerEndpoint.class,
			TestRestControllerEndpoint.class })
	static class WithRegularEndpointConfiguration {

	}

	@ControllerEndpoint(id = "testcontroller")
	static class TestControllerEndpoint {

	}

	@RestControllerEndpoint(id = "testrestcontroller")
	static class TestRestControllerEndpoint {

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

	}

	@ControllerEndpoint(id = "testcontroller")
	static class TestControllerWithOperation {

		@ReadOperation
		public String read() {
			return "error";
		}

	}

}
