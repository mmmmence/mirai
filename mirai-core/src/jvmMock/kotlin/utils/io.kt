/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.utils

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path


public fun InputStream.dropAndClose(bufferSize: Int = 2048): Unit = use {
    drop(bufferSize)
}

public fun InputStream.drop(bufferSize: Int = 2048) {
    val buffer = ByteArray(bufferSize)
    while (true) {
        if (read(buffer) == -1) {
            return
        }
    }
}

public val Path.isFile: Boolean get() = Files.exists(this) && !Files.isDirectory(this)

