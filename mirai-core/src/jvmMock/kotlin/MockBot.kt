/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock

import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.ContactList
import net.mamoe.mirai.message.data.OnlineAudio
import net.mamoe.mirai.mock.contact.MockFriend
import net.mamoe.mirai.mock.contact.MockGroup
import net.mamoe.mirai.mock.contact.MockOtherClient
import net.mamoe.mirai.mock.contact.MockStranger
import net.mamoe.mirai.mock.database.MessageDatabase
import net.mamoe.mirai.mock.fsserver.TmpFsServer
import net.mamoe.mirai.mock.userprofile.UserProfileService
import net.mamoe.mirai.mock.utils.NameGenerator
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.cast

@Suppress("unused")
@JvmBlockingBridge
public interface MockBot : Bot {
    public suspend fun doRelogin()
    public fun destroy()


    /// Contract API override
    @MockBotDSL
    override fun getFriend(id: Long): MockFriend? = super.getFriend(id)?.cast()

    @MockBotDSL
    override fun getFriendOrFail(id: Long): MockFriend = super.getFriendOrFail(id).cast()

    @MockBotDSL
    override fun getGroup(id: Long): MockGroup? = super.getGroup(id)?.cast()

    @MockBotDSL
    override fun getGroupOrFail(id: Long): MockGroup = super.getGroupOrFail(id).cast()

    @MockBotDSL
    override fun getStranger(id: Long): MockStranger? = super.getStranger(id)?.cast()

    @MockBotDSL
    override fun getStrangerOrFail(id: Long): MockStranger = super.getStrangerOrFail(id).cast()

    override val groups: ContactList<MockGroup>
    override val friends: ContactList<MockFriend>
    override val strangers: ContactList<MockStranger>
    override val otherClients: ContactList<MockOtherClient>
    override val asFriend: MockFriend
    override val asStranger: MockStranger

    /// Mock Contract API
    /// All mock api will not broadcast event

    public val nameGenerator: NameGenerator
    public val tmpFsServer: TmpFsServer
    public val msgDatabase: MessageDatabase
    public val userProfileService: UserProfileService

    @MockBotDSL
    public fun addGroup(id: Long, name: String): MockGroup

    @MockBotDSL
    public fun addGroup(id: Long, uin: Long, name: String): MockGroup

    @MockBotDSL
    public fun addFriend(id: Long, name: String): MockFriend

    @MockBotDSL
    public fun addStranger(id: Long, name: String): MockStranger

    @MockBotDSL
    public suspend fun uploadOnlineAudio(resource: ExternalResource): OnlineAudio
}
