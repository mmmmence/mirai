/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.contact

import net.mamoe.mirai.Bot
import net.mamoe.mirai.mock.MockBot
import net.mamoe.mirai.mock.MockBotFactory
import net.mamoe.mirai.mock.fsserver.TmpFsServer
import net.mamoe.mirai.mock.internal.MockBotImpl
import net.mamoe.mirai.mock.utils.NameGenerator
import net.mamoe.mirai.utils.BotConfiguration

internal class MockBotFactoryImpl : MockBotFactory {
    override fun newMockBotBuilder(): MockBotFactory.BotBuilder {
        return object : MockBotFactory.BotBuilder {
            var id: Long = 0
            lateinit var nick_: String
            lateinit var configuration_: BotConfiguration
            var nameGenerator: NameGenerator = NameGenerator.DEFAULT
            lateinit var tmpFsServer_: TmpFsServer

            override fun id(value: Long): MockBotFactory.BotBuilder = apply {
                this.id = value
            }

            override fun nick(value: String): MockBotFactory.BotBuilder = apply {
                this.nick_ = value
            }

            override fun configuration(value: BotConfiguration): MockBotFactory.BotBuilder = apply {
                this.configuration_ = value
            }

            override fun nameGenerator(value: NameGenerator): MockBotFactory.BotBuilder = apply {
                this.nameGenerator = value
            }

            override fun tmpFsServer(server: TmpFsServer): MockBotFactory.BotBuilder = apply {
                tmpFsServer_ = server
            }

            override fun createNoInstanceRegister(): MockBot {
                if (!::configuration_.isInitialized) {
                    configuration_ = BotConfiguration { }
                }
                if (!::nick_.isInitialized) {
                    nick_ = "Mock bot $id"
                }
                if (!::tmpFsServer_.isInitialized) {
                    tmpFsServer_ = TmpFsServer.newInMemoryFsServer()
                }
                return MockBotImpl(
                    configuration_,
                    id,
                    nick_,
                    nameGenerator,
                    tmpFsServer_
                )
            }

            @Suppress("INVISIBLE_MEMBER")
            override fun create(): MockBot {
                return createNoInstanceRegister().also {
                    Bot._instances[id] = it
                }
            }
        }
    }

    override fun newBot(qq: Long, password: String, configuration: BotConfiguration): Bot {
        return newMockBotBuilder()
            .id(qq)
            .configuration(configuration)
            .create()
    }

    override fun newBot(qq: Long, passwordMd5: ByteArray, configuration: BotConfiguration): Bot {
        return newMockBotBuilder()
            .id(qq)
            .configuration(configuration)
            .create()
    }
}