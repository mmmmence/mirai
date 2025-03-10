/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:Suppress("unused")

package net.mamoe.mirai.event

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.internal.event.VerboseEvent
import net.mamoe.mirai.internal.event.callAndRemoveIfRequired
import net.mamoe.mirai.internal.network.Packet
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.JavaFriendlyAPI

/**
 * 可被监听的类, 可以是任何 class 或 object.
 *
 * 若监听这个类, 监听器将会接收所有事件的广播.
 *
 * 所有 [Event] 都应继承 [AbstractEvent] 而不要直接实现 [Event]. 否则将无法广播也无法监听.
 *
 * ### 广播
 * 广播事件的唯一方式为 [broadcast].
 *
 * @see EventChannel.subscribeAlways
 * @see EventChannel.subscribeOnce
 *
 * @see EventChannel.subscribeMessages
 *
 * @see [broadcast] 广播事件
 * @see [EventChannel.subscribe] 监听事件
 *
 * @see CancellableEvent 可被取消的事件
 */
public interface Event {
    /**
     * 事件是否已被拦截.
     *
     * 所有事件都可以被拦截, 拦截后低优先级的监听器将不会处理到这个事件.
     *
     * @see intercept 拦截事件
     */
    public val isIntercepted: Boolean

    /**
     * 拦截这个事件
     *
     * 当事件被 [拦截][Event.intercept] 后, 优先级较低 (靠右) 的监听器将不会被调用.
     *
     * 优先级为 [EventPriority.MONITOR] 的监听器不应该调用这个函数.
     *
     * @see EventPriority 查看优先级相关信息
     */
    public fun intercept()
}

/**
 * 所有实现了 [Event] 接口的类都应该继承的父类.
 *
 * 在使用事件时应使用类型 [Event]. 在实现自定义事件时应继承 [AbstractEvent].
 */
public abstract class AbstractEvent : Event {
    /** 限制一个事件实例不能并行广播. (适用于 object 广播的情况) */
    @JvmField
    internal val broadCastLock = Mutex()

    @Suppress("PropertyName")
    @JvmField
    @Volatile
    internal var _intercepted = false

    @Volatile
    private var _cancelled = false

    // 实现 Event
    /**
     * @see Event.isIntercepted
     */
    public override val isIntercepted: Boolean
        get() = _intercepted

    /**
     * @see Event.intercept
     */
    public override fun intercept() {
        _intercepted = true
    }

    // 实现 CancellableEvent
    /**
     * @see CancellableEvent.isCancelled
     */
    public val isCancelled: Boolean get() = _cancelled

    /**
     * @see CancellableEvent.cancel
     */
    public fun cancel() {
        check(this is CancellableEvent) {
            "Event $this is not cancellable"
        }
        _cancelled = true
    }
}

/**
 * 可被取消的事件
 */
public interface CancellableEvent : Event {
    /**
     * 事件是否已被取消.
     *
     * 事件需实现 [CancellableEvent] 接口才可以被取消,
     * 否则此属性固定返回 false.
     */
    public val isCancelled: Boolean

    /**
     * 取消这个事件.
     * 事件需实现 [CancellableEvent] 接口才可以被取消
     *
     * @throws IllegalStateException 当事件未实现接口 [CancellableEvent] 时抛出
     */
    public fun cancel()
}

/**
 * 广播一个事件的唯一途径.
 *
 * 当事件被实现为 Kotlin `object` 时, 同一时刻只能有一个 [广播][broadcast] 存在.
 * 较晚执行的 [广播][broadcast] 将会挂起协程并等待之前的广播任务结束.
 *
 * @see __broadcastJava Java 使用
 */
@JvmSynthetic
public suspend fun <E : Event> E.broadcast(): E = _EventBroadcast.implementation.broadcastPublic(this)

/**
 * @since 2.7-M1
 */
@Suppress("ClassName")
internal open class _EventBroadcast {
    companion object {
        @Volatile
        @JvmStatic
        var implementation: _EventBroadcast = _EventBroadcast()

        private val SHOW_VERBOSE_EVENT_ALWAYS = systemProp("mirai.event.show.verbose.events", false)
    }

    open suspend fun <E : Event> broadcastPublic(event: E): E = event.apply { Mirai.broadcastEvent(this) }

    @JvmName("broadcastImpl") // avoid mangling
    internal suspend fun <E : Event> broadcastImpl(event: E): E {
        check(event is AbstractEvent) { "Events must extend AbstractEvent" }

        if (event is BroadcastControllable && !event.shouldBroadcast) {
            return event
        }
        event.broadCastLock.withLock {
            event._intercepted = false
            if (EventDisabled) return@withLock
            logEvent(event)
            callAndRemoveIfRequired(event)
        }

        return event
    }

    private fun isVerboseEvent(event: Event): Boolean {
        if (SHOW_VERBOSE_EVENT_ALWAYS) return false
        if (event is VerboseEvent) {
            if (event is BotEvent) {
                return !event.bot.configuration.isShowingVerboseEventLog
            }
            return true
        }
        return false
    }

    private fun logEvent(event: Event) {
        if (event is Packet.NoEventLog) return
        if (event is Packet.NoLog) return
        if (event is MessageEvent) return // specially handled in [LoggingPacketHandlerAdapter]
//        if (this is Packet) return@withLock // all [Packet]s are logged in [LoggingPacketHandlerAdapter]
        if (isVerboseEvent(event)) return

        if (event is BotEvent) {
            event.bot.logger.verbose { "Event: $event" }
        } else {
            topLevelEventLogger.verbose { "Event: $event" }
        }
    }

    private val topLevelEventLogger by lazy { MiraiLogger.Factory.create(Event::class, "EventPipeline") }
}

/**
 * 在 Java 广播一个事件的唯一途径.
 *
 * 调用方法: `EventKt.broadcast(event)`
 */
@Suppress("FunctionName")
@JvmName("broadcast")
@JavaFriendlyAPI
public fun <E : Event> E.__broadcastJava(): E = apply {
    if (this is BroadcastControllable && !this.shouldBroadcast) {
        return@apply
    }
    runBlocking { this@__broadcastJava.broadcast() }
}

/**
 * 设置为 `true` 以关闭事件.
 * 所有的 `subscribe` 都能正常添加到监听器列表, 但所有的广播都会直接返回.
 */
@MiraiExperimentalApi
public var EventDisabled: Boolean = false

/**
 * 可控制是否需要广播这个事件
 */
public interface BroadcastControllable : Event {
    /**
     * 返回 `false` 时将不会广播这个事件.
     */
    public val shouldBroadcast: Boolean
        get() = true
}

