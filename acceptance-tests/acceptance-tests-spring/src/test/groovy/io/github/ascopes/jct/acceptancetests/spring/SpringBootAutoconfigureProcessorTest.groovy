/*
 * Copyright (C) 2022 - 2022 Ashley Scopes
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
package io.github.ascopes.jct.acceptancetests.spring

import io.github.ascopes.jct.compilers.JctCompiler
import io.github.ascopes.jct.junit.JavacCompilerTest
import io.github.ascopes.jct.pathwrappers.TempDirectory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.boot.autoconfigureprocessor.AutoConfigureAnnotationProcessor

import static io.github.ascopes.jct.assertions.JctAssertions.assertThatCompilation
import static io.github.ascopes.jct.pathwrappers.TempDirectory.newTempDirectory
import static org.assertj.core.api.Assumptions.assumeThat

@DisplayName("Spring Boot Autoconfigure Processor acceptance tests")
class SpringBootAutoconfigureProcessorTest {

  @BeforeEach
  void ensureJdk17() {
    assumeThat(JRE.currentVersion())
        .withFailMessage(
            "This test targets Java 17 and newer"
        )
        .isGreaterThanOrEqualTo(JRE.JAVA_17)
  }

  @DisplayName("Spring will index the application context as expected")
  @Execution(ExecutionMode.CONCURRENT)
  @JavacCompilerTest(minVersion = 17)
  void springWillIndexTheApplicationContextAsExpected(JctCompiler compiler) {
    // Given
    // Use a temporary directory here as the configuration processor uses java.io.File internally.
    def sources = newTempDirectory("sources")
        .createDirectory("org", "example")
        .copyContentsFrom("src", "test", "resources", "code", "autoconfigure")

    // When
    def compilation = compiler
        .addSourcePath(sources)
        .addAnnotationProcessors(new AutoConfigureAnnotationProcessor())
        .testDirectoryFactory(TempDirectory.&newTempDirectory)
        .compile()

    // Then
    assertThatCompilation(compilation)
        .isSuccessfulWithoutWarnings()
        .classOutput()
        .packages()
        .fileExists("META-INF/spring-autoconfigure-metadata.properties")
        .isNotEmptyFile()
  }

  @DisplayName("Spring will index the application context as expected when using modules")
  @Execution(ExecutionMode.CONCURRENT)
  @JavacCompilerTest(minVersion = 17)
  void springWillIndexTheApplicationContextAsExpectedWhenUsingModules(JctCompiler compiler) {
    // Given
    // Use a temporary directory here as the configuration processor uses java.io.File internally.
    def sources = newTempDirectory("sources")
        .createDirectory("org", "example")
        .copyContentsFrom("src", "test", "resources", "code", "autoconfigure")
        .createFile("module-info.java").withContents("""
          module org.example {
            requires java.base;
            requires spring.beans;
            requires spring.boot;
            requires spring.boot.autoconfigure;
            requires spring.context;
            requires spring.core;
            requires spring.web;
            requires spring.webflux;
          }
        """.stripMargin())

    // When
    def compilation = compiler
        .addSourcePath(sources)
        .addAnnotationProcessors(new AutoConfigureAnnotationProcessor())
        .testDirectoryFactory(TempDirectory.&newTempDirectory)
        .compile()

    // Then
    assertThatCompilation(compilation)
        .isSuccessfulWithoutWarnings()
        .classOutput()
        .packages()
        .fileExists("META-INF/spring-autoconfigure-metadata.properties")
        .isNotEmptyFile()
  }
}