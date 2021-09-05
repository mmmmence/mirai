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
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.toMessageChain
import net.mamoe.mirai.mock.MockBot
import net.mamoe.mirai.mock.MockBotDSL

@JvmBlockingBridge
@Suppress("unused")
public interface MockContact : Contact {
    override val bot: MockBot

    @MockBotDSL
    public suspend infix fun says(message: MessageChain): MessageChain


    @MockBotDSL
    public suspend infix fun says(message: Message): MessageChain {
        return says(message.toMessageChain())
    }

    @MockBotDSL
    public suspend infix fun says(message: String): MessageChain {
        return says(PlainText(message))
    }
}
