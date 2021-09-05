/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */


import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.utils.addToStdlib.cast

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
    id("net.mamoe.kotlin-jvm-blocking-bridge")
}

version = Versions.project
description = "Mirai core mock testing framework"

fun realCompilation() = project(":mirai-core")
    .extensions.findByName("kotlin").cast<KotlinMultiplatformExtension>()
    .targets.first { it.name == "jvm" }
    .compilations.first { it.name == "mock" }

fun realMockCode() = realCompilation()
    .compileKotlinTask
    .outputs
    .files

dependencies {
    api(project(":mirai-core"))
    api(project(":mirai-core-api"))
    api(project(":mirai-core-utils"))
    val code = realMockCode()
    testApi(code)
    api(`ktor-server-core`)
    api(`ktor-server-netty`)
    api(`java-in-memory-file-system`)
}

// tasks.withType<KotlinJvmCompile> { enabled = false }

tasks.getByName("jar", Jar::class) {
    from(realMockCode())
}

configurePublishing("mirai-core-mock")

tasks.named("sourcesJar", Jar::class) {
    realCompilation().allKotlinSourceSets.forEach {
        from(it.kotlin)
    }
}

tasks.named("shadowJar") { enabled = false }
