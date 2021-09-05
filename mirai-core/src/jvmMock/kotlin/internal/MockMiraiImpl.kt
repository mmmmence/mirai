/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.internal

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.internal.MiraiImpl
import net.mamoe.mirai.message.action.Nudge
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.MessageSourceKind
import net.mamoe.mirai.message.data.OfflineMessageSource
import net.mamoe.mirai.message.data.OnlineMessageSource
import net.mamoe.mirai.mock.contact.MockGroup
import net.mamoe.mirai.mock.internal.contact.requireBotPermissionHigherThanThis
import net.mamoe.mirai.mock.utils.mock
import net.mamoe.mirai.mock.utils.simpleMemberInfo
import net.mamoe.mirai.utils.cast

internal class MockMiraiImpl : MiraiImpl() {
    override suspend fun solveBotInvitedJoinGroupRequestEvent(
        bot: Bot,
        eventId: Long,
        invitorId: Long,
        groupId: Long,
        accept: Boolean
    ) {
        bot.mock()
        if (accept) {
            val group = bot.addGroup(groupId, bot.nameGenerator.nextGroupName())
            group.addMember(
                simpleMemberInfo(
                    uin = 111111111,
                    permission = MemberPermission.OWNER,
                    name = "MockMember - Owner",
                    nameCard = "Custom NameCard",
                )
            ).addMember(
                simpleMemberInfo(
                    uin = 222222222,
                    permission = MemberPermission.ADMINISTRATOR,
                    name = "MockMember - Administrator",
                    nameCard = "root",
                )
            )

            group.addMember(
                simpleMemberInfo(
                    uin = bot.id,
                    permission = MemberPermission.MEMBER,
                    name = bot.nick,
                )
            )


            if (invitorId != 0L) {
                val invitor = group[invitorId] ?: kotlin.run {
                    group.addMember0(
                        simpleMemberInfo(
                            uin = invitorId,
                            permission = MemberPermission.ADMINISTRATOR,
                            name = bot.getFriend(invitorId)?.nick ?: "A random invitor",
                            nameCard = "invitor",
                        )
                    )
                }
                BotJoinGroupEvent.Invite(invitor)
            } else {
                BotJoinGroupEvent.Active(group)
            }.broadcast()
        }
    }

    override suspend fun solveMemberJoinRequestEvent(
        bot: Bot,
        eventId: Long,
        fromId: Long,
        fromNick: String,
        groupId: Long,
        accept: Boolean?,
        blackList: Boolean,
        message: String
    ) {
        if (accept == null || !accept) return // ignore

        val member = bot.getGroupOrFail(groupId).mock().addMember0(
            simpleMemberInfo(
                uin = fromId,
                name = fromNick,
                permission = MemberPermission.MEMBER
            )
        )
        MemberJoinEvent.Active(member).broadcast()
    }

    override suspend fun solveNewFriendRequestEvent(
        bot: Bot,
        eventId: Long,
        fromId: Long,
        fromNick: String,
        accept: Boolean,
        blackList: Boolean
    ) {
        if (!accept) return

        FriendAddEvent(bot.mock().addFriend(fromId, fromNick)).broadcast()
    }

    override fun getUin(contactOrBot: ContactOrBot): Long {
        if (contactOrBot is MockGroup) return contactOrBot.uin

        return super.getUin(contactOrBot)
    }

    override suspend fun muteAnonymousMember(
        bot: Bot,
        anonymousId: String,
        anonymousNick: String,
        groupId: Long,
        seconds: Int
    ) {
        // noop
    }

    override suspend fun recallFriendMessageRaw(
        bot: Bot,
        targetId: Long,
        messageIds: IntArray,
        messageInternalIds: IntArray,
        time: Int
    ): Boolean = false // No author found

    override suspend fun recallGroupMessageRaw(
        bot: Bot,
        groupCode: Long,
        messageIds: IntArray,
        messageInternalIds: IntArray
    ): Boolean = false // No author found

    override suspend fun recallGroupTempMessageRaw(
        bot: Bot,
        groupUin: Long,
        targetId: Long,
        messageIds: IntArray,
        messageInternalIds: IntArray,
        time: Int
    ): Boolean = false // No author found

    override suspend fun recallMessage(bot: Bot, source: MessageSource) {
        if (source is OnlineMessageSource) {
            when (source) {
                is OnlineMessageSource.Incoming.FromFriend,
                is OnlineMessageSource.Outgoing.ToFriend,
                -> {
                    MessageRecallEvent.FriendRecall(
                        bot = source.bot,
                        messageIds = source.ids,
                        messageInternalIds = source.internalIds,
                        messageTime = source.time,
                        operatorId = source.subject.id,
                        operator = source.subject.cast(),
                    ).broadcast()
                }
                is OnlineMessageSource.Incoming.FromGroup,
                is OnlineMessageSource.Outgoing.ToGroup,
                -> {
                    source.sender.cast<Member>().requireBotPermissionHigherThanThis("recall message")
                    MessageRecallEvent.GroupRecall(
                        bot = source.bot,
                        authorId = source.sender.id,
                        messageIds = source.ids,
                        messageInternalIds = source.internalIds,
                        messageTime = source.time,
                        operator = source.subject.cast<Group>().botAsMember,
                        group = source.subject.cast(),
                        author = source.sender.cast()
                    ).broadcast()
                }
                else -> {
                    // TODO: No Event
                }
            }
        } else {
            source as OfflineMessageSource
            when (source.kind) {
                MessageSourceKind.GROUP -> {
                    MessageRecallEvent.GroupRecall(
                        bot = bot,
                        authorId = source.fromId,
                        messageIds = source.ids,
                        messageInternalIds = source.internalIds,
                        messageTime = source.time,
                        operator = bot.getGroupOrFail(source.targetId).botAsMember,
                        group = bot.getGroupOrFail(source.targetId),
                        author = bot.getGroupOrFail(source.targetId).getOrFail(source.fromId),
                    ).broadcast()
                }
                MessageSourceKind.FRIEND -> {
                    MessageRecallEvent.FriendRecall(
                        bot = bot,
                        messageIds = source.ids,
                        messageInternalIds = source.internalIds,
                        messageTime = source.time,
                        operatorId = source.fromId,
                        operator = bot.getFriendOrFail(source.fromId),
                    ).broadcast()
                }
                MessageSourceKind.TEMP -> {
                    // TODO: No Event
                }
                MessageSourceKind.STRANGER -> {
                    // TODO: No Event
                }
            }
        }
    }

    override suspend fun sendNudge(bot: Bot, nudge: Nudge, receiver: Contact): Boolean {
        NudgeEvent(
            from = bot,
            target = nudge.target,
            subject = receiver,
            action = "戳了戳",
            suffix = ""
        ).broadcast()
        return true
    }
}
