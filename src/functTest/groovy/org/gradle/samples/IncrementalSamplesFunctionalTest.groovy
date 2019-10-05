/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.samples

import org.gradle.testkit.runner.BuildResult

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class IncrementalSamplesFunctionalTest extends AbstractSampleFunctionalSpec {
    private static final SKIPPED_TASK_OUTCOMES = [FROM_CACHE, UP_TO_DATE, SKIPPED, NO_SOURCE]

    protected void writeGroovyDslSampleUnderTest() {
        temporaryFolder.newFolder("src")
        temporaryFolder.newFile("src/README.adoc") << """
= Demo Sample

Some doc

ifndef::env-github[]
- link:{zip-base-file-name}-groovy-dsl.zip[Download Groovy DSL ZIP]
endif::[]
"""
        temporaryFolder.newFolder("src", "groovy")
        temporaryFolder.newFile("src/groovy/build.gradle") << """
            println "Hello, world!"
        """
        temporaryFolder.newFile("src/groovy/settings.gradle") << """
            rootProject.name = 'demo'
        """
    }

    protected void writeKotlinDslSampleUnderTest() {
        temporaryFolder.newFolder("src")
        temporaryFolder.newFile("src/README.adoc") << """
= Demo Sample

Some doc

ifndef::env-github[]
- link:{zip-base-file-name}-kotlin-dsl.zip[Download Kotlin DSL ZIP]
endif::[]
"""
        temporaryFolder.newFolder("src", "kotlin")
        temporaryFolder.newFile("src/kotlin/build.gradle.kts") << """
            println("Hello, world!")
        """
        temporaryFolder.newFile("src/kotlin/settings.gradle.kts") << """
            rootProject.name = "demo"
        """
    }

    def "skips sample tasks when no source"() {
        makeSingleProject()

        when:
        def result = build('assemble')

        then:
        assertSampleTasksNotExecuted(result)
        result.task(':assemble').outcome in SKIPPED_TASK_OUTCOMES
        result.task(':generateSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
        result.task(':asciidocSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
        !groovyDslZipFile.exists()
        !kotlinDslZipFile.exists()
        !new File(projectDir, 'build').exists()
    }

    def "has no kotlin dsl tasks when no source"() {
        makeSingleProject()
        writeGroovyDslSampleUnderTest()

        when:
        def result = build('assemble')

        then:
        assertOnlyGroovyDslTasksExecutedAndNotSkipped(result)
        result.task(':assemble').outcome == SUCCESS
        result.task(':generateSampleIndex').outcome == SUCCESS
        result.task(':asciidocSampleIndex').outcome == SUCCESS

        and:
        groovyDslZipFile.exists()
        !kotlinDslZipFile.exists()

        and:
        def sampleIndexFile = new File(projectDir, "build/gradle-samples/demo/index.html")
        sampleIndexFile.exists()
        sampleIndexFile.text.contains('<a href="demo-groovy-dsl.zip">')
        !sampleIndexFile.text.contains('<a href="demo-kotlin-dsl.zip">')

        and:
        def indexFile = new File(projectDir, "build/gradle-samples/index.html")
        indexFile.exists()
        indexFile.text.contains('<a href="demo/">')
    }

    def "has no groovy dsl tasks when no source"() {
        makeSingleProject()
        writeKotlinDslSampleUnderTest()

        when:
        def result = build('assemble')

        then:
        assertOnlyKotlinDslTasksExecutedAndNotSkipped(result)
        result.task(':assemble').outcome == SUCCESS
        result.task(':generateSampleIndex').outcome == SUCCESS
        result.task(':asciidocSampleIndex').outcome == SUCCESS

        and:
        !groovyDslZipFile.exists()
        kotlinDslZipFile.exists()

        and:
        def sampleIndexFile = new File(projectDir, "build/gradle-samples/demo/index.html")
        sampleIndexFile.exists()
        !sampleIndexFile.text.contains('<a href="demo-groovy-dsl.zip">')
        sampleIndexFile.text.contains('<a href="demo-kotlin-dsl.zip">')

        and:
        def indexFile = new File(projectDir, "build/gradle-samples/index.html")
        indexFile.exists()
        indexFile.text.contains('<a href="demo/">')
    }

    def "skips all tasks when no changes"() {
        makeSingleProject()
        writeSampleUnderTest()

        when:
        def result1 = build("assemble")

        then:
        assertSampleTasksExecutedAndNotSkipped(result1)
        result1.task(':assemble').outcome == SUCCESS
        result1.task(':generateSampleIndex').outcome == SUCCESS
        result1.task(':asciidocSampleIndex').outcome == SUCCESS

        when:
        def result2 = build("assemble")

        then:
        assertSampleTasksSkipped(result2)
        result2.task(':assemble').outcome in SKIPPED_TASK_OUTCOMES
        result2.task(':generateSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
        result2.task(':asciidocSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
    }

    def "executes Asciidoctor and Zip tasks when README content change"() {
        makeSingleProject()
        writeSampleUnderTest()

        when:
        def result1 = build("assemble")

        then:
        assertSampleTasksExecutedAndNotSkipped(result1)

        when:
        def result2 = build("assemble")

        then:
        assertSampleTasksSkipped(result2)

        when:
        sampleReadMeFile << "More content\n"
        def result3 = build("assemble")

        then:
        result3.task(':generateSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
        result3.task(':asciidocSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
        result3.task(':assemble').outcome == SUCCESS

        and:
        result3.task(":generateWrapperForDemoSample").outcome in SKIPPED_TASK_OUTCOMES
        result3.task(":syncDemoGroovyDslSample").outcome == SUCCESS
        result3.task(":syncDemoKotlinDslSample").outcome == SUCCESS
        result3.task(":compressDemoGroovyDslSample").outcome == SUCCESS
        result3.task(":compressDemoKotlinDslSample").outcome == SUCCESS
        result3.task(":assembleDemoSample").outcome == SUCCESS

        and:
        def sampleIndexFile = new File(projectDir, "build/gradle-samples/demo/index.html")
        sampleIndexFile.exists()
        sampleIndexFile.text.contains("More content")

        when:
        def result4 = build("assemble")

        then:
        assertSampleTasksSkipped(result4)
    }

    def "executes Asciidoctor and Zip tasks when project version change"() {
        makeSingleProject()
        writeSampleUnderTest()

        when:
        def result1 = build("assemble")

        then:
        assertSampleTasksExecutedAndNotSkipped(result1)

        and:
        def sampleIndexFile1 = new File(projectDir, "build/gradle-samples/demo/index.html")
        sampleIndexFile1.exists()
        sampleIndexFile1.text.contains('<a href="demo-groovy-dsl.zip">')
        sampleIndexFile1.text.contains('<a href="demo-kotlin-dsl.zip">')

        and:
        groovyDslZipFile.exists()
        kotlinDslZipFile.exists()
        !getGroovyDslZipFile(version: '4.2').exists()
        !getKotlinDslZipFile(version: '4.2').exists()

        when:
        def result2 = build("assemble")

        then:
        assertSampleTasksSkipped(result2)

        when:
        buildFile << "version = '4.2'\n"
        def result3 = build("assemble")

        then:
        result3.task(':generateSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
        result3.task(':asciidocSampleIndex').outcome in SKIPPED_TASK_OUTCOMES
        result3.task(':assemble').outcome == SUCCESS

        and:
        result3.task(":generateWrapperForDemoSample").outcome in SKIPPED_TASK_OUTCOMES
        result3.task(":syncDemoGroovyDslSample").outcome in SKIPPED_TASK_OUTCOMES
        result3.task(":syncDemoKotlinDslSample").outcome in SKIPPED_TASK_OUTCOMES
        result3.task(":compressDemoGroovyDslSample").outcome == SUCCESS
        result3.task(":compressDemoKotlinDslSample").outcome == SUCCESS
        result3.task(":assembleDemoSample").outcome == SUCCESS

        and:
        def sampleIndexFile2 = new File(projectDir, "build/gradle-samples/demo/index.html")
        sampleIndexFile2.exists()
        sampleIndexFile2.text.contains('<a href="demo-4.2-groovy-dsl.zip">')
        sampleIndexFile2.text.contains('<a href="demo-4.2-kotlin-dsl.zip">')

        and:
        !groovyDslZipFile.exists()
        !kotlinDslZipFile.exists()
        getGroovyDslZipFile(version: '4.2').exists()
        getKotlinDslZipFile(version: '4.2').exists()

        when:
        def result4 = build("assemble")

        then:
        assertSampleTasksSkipped(result4)
    }
    // TODO: Change gradle version, asciidoctor and zips execute
    // TODO: Change groovy script, only groovy script executes
    // TODO: Change kotlin script, only kotlin script executes
    // TODO: Add/remove sample, index asciidoctor execute
    // TODO: Output are cached

    // TODO: Explicit kotlin dsl source task are skipped if no source ... or maybe just fail saying "hey something is wrong"
    // TODO: Explicit groovy dsl source task... same as kotlin
    // TODO: Somehow fail when no groovy DSL or kotlin DSL

    private static void assertSampleTasksNotExecuted(BuildResult result) {
        assert result.task(":generateWrapperForDemoSample") == null
        assert result.task(":syncDemoGroovyDslSample") == null
        assert result.task(":syncDemoKotlinDslSample") == null
        assert result.task(":compressDemoGroovyDslSample") == null
        assert result.task(":compressDemoKotlinDslSample") == null
        assert result.task(":assembleDemoSample").outcome in SKIPPED_TASK_OUTCOMES
    }

    private static void assertSampleTasksSkipped(BuildResult result) {
        assert result.task(":generateWrapperForDemoSample").outcome in SKIPPED_TASK_OUTCOMES
        assert result.task(":syncDemoGroovyDslSample").outcome in SKIPPED_TASK_OUTCOMES
        assert result.task(":syncDemoKotlinDslSample").outcome in SKIPPED_TASK_OUTCOMES
        assert result.task(":compressDemoGroovyDslSample").outcome in SKIPPED_TASK_OUTCOMES
        assert result.task(":compressDemoKotlinDslSample").outcome in SKIPPED_TASK_OUTCOMES
        assert result.task(":assembleDemoSample").outcome in SKIPPED_TASK_OUTCOMES
    }

    protected void makeSingleProject() {
        buildFile << """
            plugins {
                id 'org.gradle.samples'
            }

            samples {
                create("demo") {
                    sampleDir = file('src')
                }
            }
        """
    }

    protected void writeSampleUnderTest() {
        temporaryFolder.newFolder("src")
        temporaryFolder.newFile("src/README.adoc") << """
= Demo Sample

Some doc

ifndef::env-github[]
- link:{zip-base-file-name}-groovy-dsl.zip[Download Groovy DSL ZIP]
- link:{zip-base-file-name}-kotlin-dsl.zip[Download Kotlin DSL ZIP]
endif::[]
"""
        writeGroovyDslSample("src");
        writeKotlinDslSample("src")
    }

    protected static void assertSampleTasksExecutedAndNotSkipped(BuildResult result) {
        assertBothDslSampleTasksExecutedAndNotSkipped(result);
    }
}
