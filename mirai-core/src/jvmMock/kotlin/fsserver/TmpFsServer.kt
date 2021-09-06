/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.fsserver

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.mock.internal.remotefile.FsServerImpl
import net.mamoe.mirai.utils.ExternalResource
import java.io.Closeable
import java.nio.file.FileSystem

@JvmBlockingBridge
public interface TmpFsServer : Closeable {
    public val httpRoot: String
    public val fsSystem: FileSystem

    /**
     * @return resource id
     */
    public suspend fun uploadFile(resource: ExternalResource): String

    public suspend fun uploadFileAndGetUrl(resource: ExternalResource): String {
        return getHttpUrl(uploadFile(resource))
    }

    public fun startup()

    public fun getHttpUrl(id: String): String {
        return httpRoot + id
    }

    public companion object {
        @JvmStatic
        public fun ofFsSystem(fs: FileSystem, port: Int = 0): TmpFsServer {
            return FsServerImpl(fs, port)
        }

        @JvmStatic
        public fun newInMemoryFsServer(port: Int = 0): TmpFsServer {
            val fs = Jimfs.newFileSystem(
                Configuration.unix()
                    .toBuilder()
                    .setWorkingDirectory("/")
                    .build()
            )
            return FsServerImpl(fs, port)
        }
    }
}
