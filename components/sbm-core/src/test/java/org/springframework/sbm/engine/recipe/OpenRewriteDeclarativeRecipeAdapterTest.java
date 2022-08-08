/*
 * Copyright 2021 - 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.sbm.engine.recipe;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.sbm.engine.context.ProjectContext;
import org.springframework.sbm.project.RewriteSourceFileWrapper;
import org.springframework.sbm.project.resource.ResourceHelper;
import org.springframework.sbm.project.resource.TestProjectContext;
import org.springframework.validation.beanvalidation.CustomValidatorBean;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {
        RecipeParser.class,
        YamlObjectMapperConfiguration.class,
        CustomValidator.class,
        ResourceHelper.class,
        ActionDeserializerRegistry.class,
        RewriteMigrationResultMerger.class,
        RewriteSourceFileWrapper.class,
        CustomValidatorBean.class
})
class OpenRewriteDeclarativeRecipeAdapterTest {

    @Autowired
    RecipeParser recipeParser;

    @Test
    void recipeFromYaml() throws IOException {
        String yaml =
                        "- name: test-recipe\n" +
                        "  description: Replace deprecated spring.datasource.* properties\n" +
                        "  condition:\n" +
                        "    type: org.springframework.sbm.common.migration.conditions.TrueCondition\n" +
                        "  actions:\n" +
                        "    - type: org.springframework.sbm.engine.recipe.OpenRewriteDeclarativeRecipeAdapter\n" +
                        "      description: Call a OpenRewrite recipe\n" +
                        "      openRewriteRecipe: |-\n" +
                        "        type: specs.openrewrite.org/v1beta/recipe\n" +
                        "        name: org.openrewrite.java.RemoveAnnotation\n" +
                        "        displayName: Order imports\n" +
                        "        description: Order imports\n" +
                        "        recipeList:\n" +
                        "          - org.openrewrite.java.RemoveAnnotation:\n" +
                        "              annotationPattern: \"@java.lang.Deprecated\"\n" +
                        "          - org.openrewrite.java.format.AutoFormat";

        Recipe[] recipes = recipeParser.parseRecipe(yaml);
        assertThat(recipes[0].getActions().get(0)).isInstanceOf(OpenRewriteDeclarativeRecipeAdapter.class);
        OpenRewriteDeclarativeRecipeAdapter recipeAdapter = (OpenRewriteDeclarativeRecipeAdapter) recipes[0].getActions().get(0);

        String javaSource = "@java.lang.Deprecated\n" +
                "public class Foo {}";

        ProjectContext context = TestProjectContext.buildProjectContext()
                .addJavaSource("src/main/java", javaSource)
                .build();

        recipeAdapter.apply(context);

        assertThat(context.getProjectJavaSources().list().get(0).print()).isEqualTo(
                "public class Foo {\n" +
                        "}"
        );
    }

    @Test
    @Disabled
    public void propagatesExceptionFromOpenRewriteSimple() throws IOException {

        String actionDescription =
                "- name: test-recipe\n" +
                        "  description: Replace deprecated spring.datasource.* properties\n" +
                        "  condition:\n" +
                        "    type: org.springframework.sbm.common.migration.conditions.TrueCondition\n" +
                        "  actions:\n" +
                        "    - type: org.springframework.sbm.engine.recipe.OpenRewriteDeclarativeRecipeAdapter\n" +
                        "      condition:\n" +
                        "        type: org.springframework.sbm.common.migration.conditions.TrueCondition\n" +
                        "        versionStartingWith: \"2.7.\"\n" +
                        "      description: Add Spring Milestone Repository and bump parent pom to 3.0.0-M3\n" +
                        "\n" +
                        "      openRewriteRecipe: |-\n" +
                        "        type: specs.openrewrite.org/v1beta/recipe\n" +
                        "        name: org.openrewrite.java.spring.boot3.data.UpgradeSpringData30\n" +
                        "        displayName: Upgrade to Spring Data 3.0\n" +
                        "        description: 'Upgrade to Spring Data to 3.0 from any prior version.'\n" +
                        "        recipeList:\n" +
                        "          - org.springframework.sbm.engine.recipe.ErrorClass\n";

        Recipe[] recipes = recipeParser.parseRecipe(actionDescription);
        assertThat(recipes[0].getActions().get(0)).isInstanceOf(OpenRewriteDeclarativeRecipeAdapter.class);
        OpenRewriteDeclarativeRecipeAdapter recipeAdapter = (OpenRewriteDeclarativeRecipeAdapter) recipes[0].getActions().get(0);

        String javaSource = "@java.lang.Deprecated\n" +
                "public class Foo {}";

        ProjectContext context = TestProjectContext.buildProjectContext()
                .addJavaSource("src/main/java", javaSource)
                .build();

        assertThrows(RuntimeException.class, () -> recipeAdapter.apply(context));

    }

    @Test
    public void propagatesExceptionFromOpenRewrite() throws IOException {
        String actionDescription =
                "- name: test-recipe\n" +
                        "  description: Replace deprecated spring.datasource.* properties\n" +
                        "  condition:\n" +
                        "    type: org.springframework.sbm.common.migration.conditions.TrueCondition\n" +
                        "  actions:\n" +
                        "    - type: org.springframework.sbm.engine.recipe.OpenRewriteDeclarativeRecipeAdapter\n" +
                        "      condition:\n" +
                        "        type: org.springframework.sbm.common.migration.conditions.TrueCondition\n" +
                        "        versionStartingWith: \"2.7.\"\n" +
                        "      description: Add Spring Milestone Repository and bump parent pom to 3.0.0-M3\n" +
                        "      openRewriteRecipe: |-\n" +
                        "        type: specs.openrewrite.org/v1beta/recipe\n" +
                        "        name: org.openrewrite.java.spring.boot3.data.UpgradeSpringData30\n" +
                        "        displayName: Upgrade to Spring Data 3.0\n" +
                        "        description: 'Upgrade to Spring Data to 3.0 from any prior version.'\n" +
                        "        recipeList:\n" +
                        "          - org.openrewrite.maven.UpgradeParentVersion:\n" +
                        "              groupId: org.springframework.boot\n" +
                        "              artifactId: spring-boot-starter-parent\n" +
                        "              newVersion: 3.0.0-M3\n" +
                        "          - org.openrewrite.maven.UpgradeDependencyVersion:\n" +
                        "              groupId: org.springframework.boot\n" +
                        "              artifactId: spring-boot-dependencies\n" +
                        "              newVersion: 3.0.0-M3";

        Recipe[] recipes = recipeParser.parseRecipe(actionDescription);
        assertThat(recipes[0].getActions().get(0)).isInstanceOf(OpenRewriteDeclarativeRecipeAdapter.class);
        OpenRewriteDeclarativeRecipeAdapter recipeAdapter = (OpenRewriteDeclarativeRecipeAdapter) recipes[0].getActions().get(0);

        ProjectContext context = TestProjectContext.buildProjectContext()
                .withMavenRootBuildFileSource(pomWithError)
                .build();

        assertThrows(RuntimeException.class, () -> recipeAdapter.apply(context));
    }


    String pomWithError = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.springframework.samples</groupId>
              <artifactId>spring-petclinic</artifactId>
              <version>2.7.0-SNAPSHOT</version>
                        
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.7.1</version>
              </parent>
              <name>petclinic</name>
                        
              <properties>
                        
                <!-- Generic properties -->
                <java.version>1.8</java.version>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                        
                <!-- Web dependencies -->
                <webjars-bootstrap.version>5.1.3</webjars-bootstrap.version>
                <webjars-font-awesome.version>4.7.0</webjars-font-awesome.version>
                        
                <jacoco.version>0.8.7</jacoco.version>
                <nohttp-checkstyle.version>0.0.10</nohttp-checkstyle.version>
                <spring-format.version>0.0.31</spring-format.version>
                        
              </properties>
                        
              <dependencies>
                <!-- Spring and Spring Boot dependencies -->
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-actuator</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-cache</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-data-jpa</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-web</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-validation</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-thymeleaf</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
                        
                <!-- Databases - Uses H2 by default -->
                <dependency>
                  <groupId>com.h2database</groupId>
                  <artifactId>h2</artifactId>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>mysql</groupId>
                  <artifactId>mysql-connector-java</artifactId>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>org.postgresql</groupId>
                  <artifactId>postgresql</artifactId>
                  <scope>runtime</scope>
                </dependency>
                        
                <!-- caching -->
                <dependency>
                  <groupId>javax.cache</groupId>
                  <artifactId>cache-api</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.ehcache</groupId>
                  <artifactId>ehcache</artifactId>
                </dependency>
                        
                <!-- webjars -->
                <dependency>
                  <groupId>org.webjars</groupId>
                  <artifactId>webjars-locator-core</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.webjars.npm</groupId>
                  <artifactId>bootstrap</artifactId>
                  <version>${webjars-bootstrap.version}</version>
                </dependency>
                <dependency>
                  <groupId>org.webjars.npm</groupId>
                  <artifactId>font-awesome</artifactId>
                  <version>${webjars-font-awesome.version}</version>
                </dependency>
                <!-- end of webjars -->
                        
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-devtools</artifactId>
                  <optional>true</optional>
                </dependency>
              </dependencies>
                        
              <build>
                <plugins>
                  <plugin>
                    <groupId>io.spring.javaformat</groupId>
                    <artifactId>spring-javaformat-maven-plugin</artifactId>
                    <version>${spring-format.version}</version>
                    <executions>
                      <execution>
                        <phase>validate</phase>
                        <goals>
                          <goal>validate</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-checkstyle-plugin</artifactId>
                    <version>3.1.2</version>
                    <dependencies>
                      <dependency>
                        <groupId>com.puppycrawl.tools</groupId>
                        <artifactId>checkstyle</artifactId>
                        <version>8.45.1</version>
                      </dependency>
                      <dependency>
                        <groupId>io.spring.nohttp</groupId>
                        <artifactId>nohttp-checkstyle</artifactId>
                        <version>${nohttp-checkstyle.version}</version>
                      </dependency>
                    </dependencies>
                    <executions>
                      <execution>
                        <id>nohttp-checkstyle-validation</id>
                        <phase>validate</phase>
                        <configuration>
                          <configLocation>src/checkstyle/nohttp-checkstyle.xml</configLocation>
                          <suppressionsLocation>src/checkstyle/nohttp-checkstyle-suppressions.xml</suppressionsLocation>
                          <encoding>UTF-8</encoding>
                          <sourceDirectories>${basedir}</sourceDirectories>
                          <includes>**/*</includes>
                          <excludes>**/.git/**/*,**/.idea/**/*,**/target/**/,**/.flattened-pom.xml,**/*.class</excludes>
                        </configuration>
                        <goals>
                          <goal>check</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <executions>
                      <execution>
                        <!-- Spring Boot Actuator displays build-related information
                          if a META-INF/build-info.properties file is present -->
                        <goals>
                          <goal>build-info</goal>
                        </goals>
                        <configuration>
                          <additionalProperties>
                            <encoding.source>${project.build.sourceEncoding}</encoding.source>
                            <encoding.reporting>${project.reporting.outputEncoding}</encoding.reporting>
                            <java.source>${maven.compiler.source}</java.source>
                            <java.target>${maven.compiler.target}</java.target>
                          </additionalProperties>
                        </configuration>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>${jacoco.version}</version>
                    <executions>
                      <execution>
                        <goals>
                          <goal>prepare-agent</goal>
                        </goals>
                      </execution>
                      <execution>
                        <id>report</id>
                        <phase>prepare-package</phase>
                        <goals>
                          <goal>report</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                        
                  <!-- Spring Boot Actuator displays build-related information if a git.properties
                    file is present at the classpath -->
                  <plugin>
                    <groupId>pl.project13.maven</groupId>
                    <artifactId>git-commit-id-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>revision</goal>
                        </goals>
                      </execution>
                    </executions>
                    <configuration>
                      <verbose>true</verbose>
                      <dateFormat>yyyy-MM-dd'T'HH:mm:ssZ</dateFormat>
                      <generateGitPropertiesFile>true</generateGitPropertiesFile>
                      <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties
                      </generateGitPropertiesFilename>
                      <failOnNoGitDirectory>false</failOnNoGitDirectory>
                      <failOnUnableToExtractRepoInfo>false</failOnUnableToExtractRepoInfo>
                    </configuration>
                  </plugin>
                        
                </plugins>
              </build>
                        
              <licenses>
                <license>
                  <name>Apache License, Version 2.0</name>
                  <url>https://www.apache.org/licenses/LICENSE-2.0</url>
                </license>
              </licenses>
                        
              <repositories>
                <repository>
                  <id>spring-snapshots</id>
                  <name>Spring Snapshots</name>
                  <url>https://repo.spring.io/snapshot</url>
                  <snapshots>
                    <enabled>true</enabled>
                  </snapshots>
                </repository>
                <repository>
                  <id>spring-milestones</id>
                  <name>Spring Milestones</name>
                  <url>https://repo.spring.io/milestone</url>
                  <snapshots>
                    <enabled>false</enabled>
                  </snapshots>
                </repository>
              </repositories>
                        
              <pluginRepositories>
                <pluginRepository>
                  <id>spring-snapshots</id>
                  <name>Spring Snapshots</name>
                  <url>https://repo.spring.io/snapshot</url>
                  <snapshots>
                    <enabled>true</enabled>
                  </snapshots>
                </pluginRepository>
                <pluginRepository>
                  <id>spring-milestones</id>
                  <name>Spring Milestones</name>
                  <url>https://repo.spring.io/milestone</url>
                  <snapshots>
                    <enabled>false</enabled>
                  </snapshots>
                </pluginRepository>
              </pluginRepositories>
                        
              <profiles>
                <profile>
                  <id>css</id>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-dependency-plugin</artifactId>
                        <executions>
                          <execution>
                            <id>unpack</id>
                            <?m2e execute onConfiguration,onIncremental?>
                            <phase>generate-resources</phase>
                            <goals>
                              <goal>unpack</goal>
                            </goals>
                            <configuration>
                              <artifactItems>
                                <artifactItem>
                                  <groupId>org.webjars.npm</groupId>
                                  <artifactId>bootstrap</artifactId>
                                  <version>${webjars-bootstrap.version}</version>
                                </artifactItem>
                              </artifactItems>
                              <outputDirectory>${project.build.directory}/webjars</outputDirectory>
                            </configuration>
                          </execution>
                        </executions>
                      </plugin>
                        
                      <plugin>
                        <groupId>com.gitlab.haynes</groupId>
                        <artifactId>libsass-maven-plugin</artifactId>
                        <version>0.2.26</version>
                        <executions>
                          <execution>
                            <phase>generate-resources</phase>
                            <?m2e execute onConfiguration,onIncremental?>
                            <goals>
                              <goal>compile</goal>
                            </goals>
                          </execution>
                        </executions>
                        <configuration>
                          <inputPath>${basedir}/src/main/scss/</inputPath>
                          <outputPath>${basedir}/src/main/resources/static/resources/css/</outputPath>
                          <includePath>${project.build.directory}/webjars/META-INF/resources/webjars/bootstrap/${webjars-bootstrap.version}/scss/</includePath>
                        </configuration>
                      </plugin>
                    </plugins>
                  </build>
                </profile>
                <profile>
                  <id>m2e</id>
                  <activation>
                    <property>
                      <name>m2e.version</name>
                    </property>
                  </activation>
                  <build>
                    <pluginManagement>
                      <plugins>
                        <!-- This plugin's configuration is used to store Eclipse m2e settings
               only. It has no influence on the Maven build itself. -->
                        <plugin>
                          <groupId>org.eclipse.m2e</groupId>
                          <artifactId>lifecycle-mapping</artifactId>
                          <version>1.0.0</version>
                          <configuration>
                            <lifecycleMappingMetadata>
                              <pluginExecutions>
                                <pluginExecution>
                                  <pluginExecutionFilter>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-checkstyle-plugin</artifactId>
                                    <versionRange>[1,)</versionRange>
                                    <goals>
                                      <goal>check</goal>
                                    </goals>
                                  </pluginExecutionFilter>
                                  <action>
                                    <ignore />
                                  </action>
                                </pluginExecution>
                                <pluginExecution>
                                  <pluginExecutionFilter>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-maven-plugin</artifactId>
                                    <versionRange>[1,)</versionRange>
                                    <goals>
                                      <goal>build-info</goal>
                                    </goals>
                                  </pluginExecutionFilter>
                                  <action>
                                    <ignore />
                                  </action>
                                </pluginExecution>
                                <pluginExecution>
                                  <pluginExecutionFilter>
                                    <groupId>io.spring.javaformat</groupId>
                                    <artifactId>spring-javaformat-maven-plugin</artifactId>
                                    <versionRange>[0,)</versionRange>
                                    <goals>
                                      <goal>validate</goal>
                                    </goals>
                                  </pluginExecutionFilter>
                                  <action>
                                    <ignore />
                                  </action>
                                </pluginExecution>
                              </pluginExecutions>
                            </lifecycleMappingMetadata>
                          </configuration>
                        </plugin>
                      </plugins>
                    </pluginManagement>
                  </build>
                </profile>
              </profiles>
                        
            </project>
            """;
}
