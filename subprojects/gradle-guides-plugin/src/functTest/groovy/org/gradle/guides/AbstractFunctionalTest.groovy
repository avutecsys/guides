/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.guides

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class AbstractFunctionalTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()
    TestFile projectDir
    TestFile buildFile
    TestFile settingsFile
    BuildResult result
    private String gradleVersion

    def setup() {
        projectDir = new TestFile(temporaryFolder.root)
        buildFile = new TestFile(temporaryFolder.newFile('build.gradle'))
        settingsFile = new TestFile(temporaryFolder.newFile('settings.gradle'))
        file("gradle.properties").text = "org.gradle.jvmargs=-XX:MaxMetaspaceSize=500m -Xmx500m"
    }

    protected BuildResult build(String... arguments) {
        result = createAndConfigureGradleRunner(arguments).build()
        result
    }

    protected BuildResult buildAndFail(String... arguments) {
        result = createAndConfigureGradleRunner(arguments).buildAndFail()
        result
    }

    private GradleRunner createAndConfigureGradleRunner(String... arguments) {
        def allArgs = (arguments as List) + ["-S"]
        def runner = GradleRunner.create().withProjectDir(projectDir).withArguments(allArgs).withPluginClasspath().forwardOutput().withDebug(true)
        if (gradleVersion != null) {
            runner.withGradleVersion(gradleVersion)
            gradleVersion = null
        }
        return runner
    }

    void usingGradleVersion(String gradleVersion) {
        this.gradleVersion = gradleVersion
    }

    static File createDir(File dir, String subDirName) {
        File newDir = new File(dir, subDirName)

        if (!newDir.mkdirs()) {
            throw new IOException("Unable to create directory " + subDirName)
        }

        newDir
    }

    TestFile file(Object... paths) {
        return projectDir.file(paths)
    }
}
