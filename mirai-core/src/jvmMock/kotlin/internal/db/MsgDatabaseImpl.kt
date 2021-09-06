/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.internal.db

import net.mamoe.mirai.message.data.MessageSourceKind
import net.mamoe.mirai.mock.database.MessageDatabase
import net.mamoe.mirai.mock.database.MessageInfo
import net.mamoe.mirai.mock.database.mockMsgDatabaseId
import net.mamoe.mirai.utils.currentTimeSeconds
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

internal class MsgDatabaseImpl : MessageDatabase {
    override fun disconnect() {}
    override fun connect() {}

    val db = ConcurrentLinkedDeque<MessageInfo>()
    val idCounter1 = AtomicInteger(Random.nextInt())
    val idCounter2 = AtomicInteger(Random.nextInt())

    override fun newMessageInfo(sender: Long, subject: Long, kind: MessageSourceKind): MessageInfo {
        val dbid = mockMsgDatabaseId(idCounter1.getAndIncrement(), idCounter2.getAndDecrement())
        val info = MessageInfo(
            msgId = dbid,
            sender = sender,
            subject = subject,
            kind = kind,
            time = currentTimeSeconds(),
        )
        db.add(info)
        return info
    }

    override fun queryMessageInfo(msgId: Long): MessageInfo? {
        return db.firstOrNull { it.msgId == msgId }
    }

    override fun removeMessageInfo(msgId: Long) {
        db.removeIf { it.msgId == msgId }
    }
}