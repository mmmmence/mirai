/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.internal.remotefile

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.mamoe.mirai.mock.fsserver.TmpFsServer
import net.mamoe.mirai.mock.utils.isFile
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.currentTimeMillis
import net.mamoe.mirai.utils.runBIO
import net.mamoe.mirai.utils.useAutoClose
import java.net.ServerSocket
import java.net.URI
import java.nio.file.FileSystem
import java.util.*
import kotlin.io.path.createFile
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

internal class FsServerImpl(
    override val fsSystem: FileSystem,
    val httpPort: Int,
) : TmpFsServer {
    override lateinit var httpRoot: String
    lateinit var server: NettyApplicationEngine

    override suspend fun uploadFile(resource: ExternalResource): String {
        val fid = "${currentTimeMillis()}-${UUID.randomUUID()}"
        resource.useAutoClose { res ->
            runBIO {
                fsSystem.getPath(fid).also {
                    it.createFile()
                }.outputStream().use { fso ->
                    res.inputStream().buffered().use { it.copyTo(fso) }
                }
            }
        }
        return fid
    }

    override fun startup() {
        val port = if (httpPort == 0) {
            ServerSocket(0).use { it.localPort }
        } else {
            httpPort
        }
        httpRoot = "http://127.0.0.1:$port/"

        val server = embeddedServer(Netty, environment = applicationEngineEnvironment {
            connector {
                this.host = "127.0.0.1"
                this.port = port
            }
            module {
                intercept(ApplicationCallPipeline.Call) {
                    val path = fsSystem.getPath(URI.create(call.request.origin.uri).path.removePrefix("/"))
                    if (path.isFile) {
                        call.respondOutputStream {
                            runBIO {
                                path.inputStream().buffered().use { it.copyTo(this) }
                            }
                        }
                        finish()
                    }
                }
            }
        })
        this.server = server
        server.start(false)
    }

    override fun close() {
        if (this::server.isInitialized) {
            server.stop(0, 0)
        }
        fsSystem.close()
    }
}