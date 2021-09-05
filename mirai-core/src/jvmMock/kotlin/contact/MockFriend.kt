/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.contact

import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.FriendAddEvent
import net.mamoe.mirai.mock.MockBotDSL
import kotlin.random.Random

@JvmBlockingBridge
public interface MockFriend : Friend, MockContact {
    override var nick: String
    override var remark: String

    @MockBotDSL
    public suspend fun broadcastFriendAddEvent(): FriendAddEvent {
        return FriendAddEvent(this).broadcast()
    }

    @MockBotDSL
    public suspend fun broadcastInviteBotJoinGroupRequestEvent(
        groupId: Long, groupName: String,
    ): BotInvitedJoinGroupRequestEvent {
        return BotInvitedJoinGroupRequestEvent(
            bot,
            Random.nextLong(),
            id,
            groupId,
            groupName,
            nick
        ).broadcast()
    }

    @MockBotDSL
    public suspend fun broadcastFriendDelete() {
        delete()
    }
}