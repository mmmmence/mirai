/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.internal.message

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.modules.*
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.message.MessageSerializers
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.mirai.utils.lateinitMutableProperty
import net.mamoe.mirai.utils.map
import net.mamoe.mirai.utils.takeElementsFrom
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf

@MiraiInternalApi
public open class MessageSourceSerializerImpl(serialName: String) :
    KSerializer<MessageSource> by SerialData.serializer().map(
        resultantDescriptor = buildClassSerialDescriptor(serialName) {
            takeElementsFrom(SerialData.serializer().descriptor)
        },
        serialize = {
            SerialData(kind, botId, ids, internalIds, time, fromId, targetId, originalMessage)
        },
        deserialize = {
            Mirai.constructMessageSource(botId, kind, fromId, targetId, ids, time, internalIds, originalMessage)
        }
    ) {
    @SerialName(MessageSource.SERIAL_NAME)
    @Serializable
    internal class SerialData(
        val kind: MessageSourceKind,
        val botId: Long,
        val ids: IntArray,
        val internalIds: IntArray,
        val time: Int,
        val fromId: Long,
        val targetId: Long,
        val originalMessage: MessageChain,
    )
}


private val builtInSerializersModule by lazy {
    SerializersModule {
        // NOTE: contextual serializers disabled because of https://github.com/mamoe/mirai/issues/951

//        // non-Message classes
//        contextual(RawForwardMessage::class, RawForwardMessage.serializer())
//        contextual(ForwardMessage.Node::class, ForwardMessage.Node.serializer())
//        contextual(VipFace.Kind::class, VipFace.Kind.serializer())
//
//
//        // In case Proguard or something else obfuscated the Kotlin metadata, providing the serializers explicitly will help.
//        contextual(At::class, At.serializer())
//        contextual(AtAll::class, AtAll.serializer())
//        contextual(CustomMessage::class, CustomMessage.serializer())
//        contextual(CustomMessageMetadata::class, CustomMessageMetadata.serializer())
//        contextual(Face::class, Face.serializer())
//        contextual(Image::class, Image.Serializer)
//        contextual(PlainText::class, PlainText.serializer())
//        contextual(QuoteReply::class, QuoteReply.serializer())
//
//        contextual(ForwardMessage::class, ForwardMessage.serializer())
//
//
//        contextual(LightApp::class, LightApp.serializer())
//        contextual(SimpleServiceMessage::class, SimpleServiceMessage.serializer())
//        contextual(AbstractServiceMessage::class, AbstractServiceMessage.serializer())
//
//        contextual(PttMessage::class, PttMessage.serializer())
//        contextual(Voice::class, Voice.serializer())
//        contextual(PokeMessage::class, PokeMessage.serializer())
//        contextual(VipFace::class, VipFace.serializer())
//        contextual(FlashImage::class, FlashImage.serializer())
//
//        contextual(MusicShare::class, MusicShare.serializer())
//
//        contextual(MessageSource::class, MessageSource.serializer())

//        contextual(SingleMessage::class, SingleMessage.Serializer)
        contextual(MessageChain::class, MessageChain.Serializer)
        contextual(MessageChainImpl::class, MessageChainImpl.serializer())

        contextual(ShowImageFlag::class, ShowImageFlag.Serializer)

        contextual(MessageOriginKind::class, MessageOriginKind.serializer())

        fun PolymorphicModuleBuilder<MessageMetadata>.messageMetadataSubclasses() {
            subclass(MessageSource::class, MessageSource.serializer())
            subclass(QuoteReply::class, QuoteReply.serializer())
            subclass(ShowImageFlag::class, ShowImageFlag.Serializer)
            subclass(MessageOrigin::class, MessageOrigin.serializer())
        }

        fun PolymorphicModuleBuilder<MessageContent>.messageContentSubclasses() {
            subclass(At::class, At.serializer())
            subclass(AtAll::class, AtAll.serializer())
            subclass(Face::class, Face.serializer())
            subclass(Image::class, Image.Serializer)
            subclass(PlainText::class, PlainText.serializer())

            subclass(ForwardMessage::class, ForwardMessage.serializer())


            subclass(LightApp::class, LightApp.serializer())
            subclass(SimpleServiceMessage::class, SimpleServiceMessage.serializer())

            //  subclass(PttMessage::class, PttMessage.serializer())
            @Suppress("DEPRECATION")
            subclass(Voice::class, Voice.serializer())

            // subclass(HummerMessage::class, HummerMessage.serializer())
            subclass(PokeMessage::class, PokeMessage.serializer())
            subclass(VipFace::class, VipFace.serializer())
            subclass(FlashImage::class, FlashImage.serializer())

            subclass(MusicShare::class, MusicShare.serializer())

            subclass(Dice::class, Dice.serializer())
        }


        //  polymorphic(SingleMessage::class) {
        //      subclass(MessageSource::class, MessageSource.serializer())
        //      default {
        //          Message.Serializer.serializersModule.getPolymorphic(Message::class, it)
        //      }
        //  }

        // polymorphic(Message::class) {
        //     subclass(PlainText::class, PlainText.serializer())
        // }
        polymorphic(SingleMessage::class) {
            messageContentSubclasses()
            messageMetadataSubclasses()
        }

        polymorphic(MessageContent::class) {
            messageContentSubclasses()
        }

        polymorphic(MessageMetadata::class) {
            messageMetadataSubclasses()
        }

        polymorphic(RichMessage::class) {
            subclass(SimpleServiceMessage::class, SimpleServiceMessage.serializer())
            subclass(LightApp::class, LightApp.serializer())
        }

        polymorphic(ServiceMessage::class) {
            subclass(SimpleServiceMessage::class, SimpleServiceMessage.serializer())
        }

        //contextual(SingleMessage::class, SingleMessage.Serializer)
        // polymorphic(SingleMessage::class, SingleMessage.Serializer) {
        //     messageContentSubclasses()
        //     messageMetadataSubclasses()
        //     singleMessageSubclasses()
        // }

        // contextual(MessageContent::class, MessageContent.Serializer)
        // polymorphic(MessageContent::class, MessageContent.Serializer) {
        //     messageContentSubclasses()
        // }

        // contextual(MessageMetadata::class, MessageMetadata.Serializer)
        // polymorphic(MessageMetadata::class, MessageMetadata.Serializer) {
        //     messageMetadataSubclasses()
        //
    }
}

// Tests:
// net.mamoe.mirai.internal.message.data.MessageSerializationTest
internal object MessageSerializersImpl : MessageSerializers {
    private var serializersModuleField: SerializersModule by lateinitMutableProperty {
        builtInSerializersModule
    }

    override val serializersModule: SerializersModule
        get() {
            Mirai // ensure registered, for tests
            return serializersModuleField
        }

    @Synchronized
    override fun <M : SingleMessage> registerSerializer(type: KClass<M>, serializer: KSerializer<M>) {
        serializersModuleField = serializersModule.overwritePolymorphicWith(type, serializer)
    }

    @Synchronized
    override fun registerSerializers(serializersModule: SerializersModule) {
        serializersModuleField = serializersModule.overwriteWith(serializersModule)
    }
}

internal fun <M : Any> SerializersModule.overwritePolymorphicWith(
    type: KClass<M>,
    serializer: KSerializer<M>
): SerializersModule {
    return overwriteWith(SerializersModule {
        // contextual(type, serializer)
        for (superclass in type.allSuperclasses) {
            if (superclass.isFinal) continue
            if (!superclass.isSubclassOf(SingleMessage::class)) continue
            @Suppress("UNCHECKED_CAST")
            polymorphic(superclass as KClass<Any>) {
                subclass(type, serializer)
            }
        }
    })
}

private inline fun <reified M : SingleMessage> SerializersModuleBuilder.hierarchicallyPolymorphic(serializer: KSerializer<M>) =
    hierarchicallyPolymorphic(M::class, serializer)

private fun <M : SingleMessage> SerializersModuleBuilder.hierarchicallyPolymorphic(
    type: KClass<M>,
    serializer: KSerializer<M>
) {
    // contextual(type, serializer)
    for (superclass in type.allSuperclasses) {
        if (superclass.isFinal) continue
        if (!superclass.isSubclassOf(SingleMessage::class)) continue
        @Suppress("UNCHECKED_CAST")
        polymorphic(superclass as KClass<Any>) {
            subclass(type, serializer)
        }
    }
}