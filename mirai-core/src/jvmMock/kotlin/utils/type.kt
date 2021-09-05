/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

@file:Suppress("NOTHING_TO_INLINE")

package net.mamoe.mirai.mock.utils

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.message.action.Nudge
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.OnlineMessageSource
import net.mamoe.mirai.message.data.source
import net.mamoe.mirai.mock.MockBot
import net.mamoe.mirai.mock.MockBotDSL
import net.mamoe.mirai.mock.contact.*
import net.mamoe.mirai.utils.cast
import java.util.*
import kotlin.contracts.contract


public fun Bot.mock(): MockBot {
    contract { returns() implies (this@mock is MockBot) }
    return this as MockBot
}

public fun Group.mock(): MockGroup {
    contract { returns() implies (this@mock is MockGroup) }
    return this as MockGroup
}

public fun NormalMember.mock(): MockNormalMember {
    contract { returns() implies (this@mock is MockNormalMember) }
    return this as MockNormalMember
}

public fun Contact.mock(): MockContact {
    contract { returns() implies (this@mock is MockContact) }
    return this as MockContact
}

public fun AnonymousMember.mock(): MockAnonymousMember {
    contract { returns() implies (this@mock is MockAnonymousMember) }
    return this as MockAnonymousMember
}

public fun Friend.mock(): MockFriend {
    contract { returns() implies (this@mock is MockFriend) }
    return this as MockFriend
}

public fun Member.mock(): MockMember {
    contract { returns() implies (this@mock is MockMember) }
    return this as MockMember
}

public fun OtherClient.mock(): MockOtherClient {
    contract { returns() implies (this@mock is MockOtherClient) }
    return this as MockOtherClient
}

public fun Stranger.mock(): MockStranger {
    contract { returns() implies (this@mock is MockStranger) }
    return this as MockStranger
}

@MockBotDSL
public inline infix fun MockBot.group(value: Long): MockGroup = getGroupOrFail(value)

@MockBotDSL
public inline infix fun MockBot.friend(value: Long): MockFriend = getFriendOrFail(value)

@MockBotDSL
public inline infix fun MockBot.stranger(value: Long): MockStranger = getStrangerOrFail(value)

@MockBotDSL
public inline infix fun MockGroup.member(id: Long): MockNormalMember = getOrFail(id)

@MockBotDSL
public inline infix fun MockGroup.anonymous(name: String): MockAnonymousMember =
    newAnonymous(name, UUID.randomUUID().toString())


/**
 * 令 [actor] 发起戳一戳, 戳一戳的发起者为 [actor], 被戳者为 [Nudge.target]
 */
@MockBotDSL
public suspend infix fun Nudge.startAction(actor: UserOrBot) {
    val target = this.target
    NudgeEvent(
        actor,
        target,
        when (target) {
            is Member -> target.group
            is Friend -> target
            is Stranger -> target
            is Bot -> {
                when (actor) {
                    is Bot -> error("Can't send BotNudged by bot-self")
                    is Friend -> actor
                    is Member -> actor.group
                    is Stranger -> actor
                    else -> error("STUB")
                }
            }
            else -> error("STUB")
        },
        "戳了戳",
        ""
    ).broadcast()
}

@MockBotDSL
public suspend fun MessageChain.mockFireRecalled(operator: Contact? = null) {
    val source = this.source
    if (source is OnlineMessageSource) {
        val from = source.sender
        when (val target = source.target) {
            is Group -> {
                MessageRecallEvent.GroupRecall(
                    source.bot,
                    from.id,
                    source.ids,
                    source.internalIds,
                    source.time,
                    operator?.cast(),
                    target,
                    from.cast()
                ).broadcast()
                return
            }
            is Friend -> {
                MessageRecallEvent.FriendRecall(
                    source.bot,
                    source.ids,
                    source.internalIds,
                    source.time,
                    from.id,
                    from.cast()
                ).broadcast()
                return
            }
        }
    }
    error("Unsupported message source type: ${source.javaClass}")
}
