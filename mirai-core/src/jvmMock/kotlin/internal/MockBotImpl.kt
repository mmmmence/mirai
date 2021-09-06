/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.internal

import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import net.mamoe.mirai.Bot
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.contact.ContactList
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.BotReloginEvent
import net.mamoe.mirai.internal.BotWithComponents
import net.mamoe.mirai.internal.message.OnlineAudioImpl
import net.mamoe.mirai.internal.network.component.ComponentStorage
import net.mamoe.mirai.internal.network.component.ConcurrentComponentStorage
import net.mamoe.mirai.internal.network.components.EventDispatcher
import net.mamoe.mirai.message.data.AudioCodec
import net.mamoe.mirai.message.data.OnlineAudio
import net.mamoe.mirai.mock.MockBot
import net.mamoe.mirai.mock.contact.MockFriend
import net.mamoe.mirai.mock.contact.MockGroup
import net.mamoe.mirai.mock.contact.MockOtherClient
import net.mamoe.mirai.mock.contact.MockStranger
import net.mamoe.mirai.mock.database.MessageDatabase
import net.mamoe.mirai.mock.fsserver.TmpFsServer
import net.mamoe.mirai.mock.internal.components.MockEventDispatcherImpl
import net.mamoe.mirai.mock.internal.contact.MockFriendImpl
import net.mamoe.mirai.mock.internal.contact.MockGroupImpl
import net.mamoe.mirai.mock.internal.contact.MockStrangerImpl
import net.mamoe.mirai.mock.utils.NameGenerator
import net.mamoe.mirai.mock.utils.simpleMemberInfo
import net.mamoe.mirai.utils.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

internal class MockBotImpl(
    override val configuration: BotConfiguration,
    override val id: Long,
    override val nick: String,
    override val nameGenerator: NameGenerator,
    override val tmpFsServer: TmpFsServer,
    override val msgDatabase: MessageDatabase
) : MockBot, BotWithComponents {
    init {
        tmpFsServer.startup()
    }

    override val components: ComponentStorage by lazy {
        ConcurrentComponentStorage {
            set(EventDispatcher, MockEventDispatcherImpl(coroutineContext, logger))
        }
    }

    @TestOnly
    internal suspend fun joinEventBroadcast() {
        components[EventDispatcher].joinBroadcast()
    }

    override suspend fun login() {
        BotOnlineEvent(this).broadcast()
    }

    override suspend fun doRelogin() {
        BotReloginEvent(this, null).broadcast()
        BotOnlineEvent(this).broadcast()
    }

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    override fun destroy() {
        tmpFsServer.close()
        Bot._instances.remove(id, this)
        cancel(CancellationException("Bot destroy"))
    }

    override val groups: ContactList<MockGroup> = ContactList()
    override val friends: ContactList<MockFriend> = ContactList()
    override val strangers: ContactList<MockStranger> = ContactList()
    override val otherClients: ContactList<MockOtherClient> = ContactList()

    override fun addGroup(id: Long, name: String): MockGroup =
        addGroup(id, Mirai.calculateGroupUinByGroupCode(id), name)

    override fun addGroup(id: Long, uin: Long, name: String): MockGroup {
        val group = MockGroupImpl(coroutineContext, this, id, uin, name)
        groups.delegate.add(group)
        group.addMember(simpleMemberInfo(this.id, this.nick, permission = MemberPermission.OWNER))
        return group
    }

    override fun addFriend(id: Long, name: String): MockFriend {
        val friend = MockFriendImpl(coroutineContext, this, id, name, "")
        friends.delegate.add(friend)
        return friend
    }

    override fun addStranger(id: Long, name: String): MockStranger {
        val stranger = MockStrangerImpl(coroutineContext, this, id, "", name)
        strangers.delegate.add(stranger)
        return stranger
    }

    override val logger: MiraiLogger by lazy {
        configuration.botLoggerSupplier(this)
    }

    override val isOnline: Boolean get() = isActive
    override val eventChannel: EventChannel<BotEvent> =
        GlobalEventChannel.filterIsInstance<BotEvent>().filter { it.bot === this@MockBotImpl }

    override val asFriend: MockFriend by lazy {
        MockFriendImpl(coroutineContext, this, id, nick, "")
    }
    override val asStranger: MockStranger by lazy {
        MockStrangerImpl(coroutineContext, this, id, "", nick)
    }

    override fun close(cause: Throwable?) {
        cancel(when (cause) {
            null -> null
            else -> CancellationException(cause.message).also { it.initCause(cause) }
        })
    }

    override val coroutineContext: CoroutineContext by lazy {
        configuration.parentCoroutineContext.childScopeContext()
    }

    override suspend fun uploadOnlineAudio(resource: ExternalResource): OnlineAudio {
        val md5 = resource.md5
        val size = resource.size
        val id = tmpFsServer.uploadFile(resource)
        return OnlineAudioImpl(
            filename = "$id.amr",
            fileMd5 = md5,
            fileSize = size,
            codec = AudioCodec.SILK,
            url = tmpFsServer.getHttpUrl(id),
            length = size,
            originalPtt = null
        )
    }
}