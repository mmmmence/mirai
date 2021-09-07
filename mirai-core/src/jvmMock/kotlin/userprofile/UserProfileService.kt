/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.userprofile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.data.UserProfile
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@JvmBlockingBridge
public interface UserProfileService {
    public suspend fun doQueryUserProfile(id: Long): UserProfile

    public suspend fun putUserProfile(id: Long, profile: UserProfile)

    public companion object {
        @JvmStatic
        public fun newDefaultInstance(): UserProfileService {
            return UserProfileServiceImpl()
        }
    }
}

@Suppress("CONFLICTING_OVERLOADS", "ILLEGAL_JVM_NAME", "INAPPLICABLE_JVM_NAME")
public interface UserProfileServiceJ : UserProfileService {
    override suspend fun doQueryUserProfile(id: Long): UserProfile {
        return withContext(Dispatchers.IO) {
            doQueryUserProfileJ(id) ?: buildUserProfile { }
        }
    }

    override suspend fun putUserProfile(id: Long, profile: UserProfile) {
        withContext(Dispatchers.IO) {
            putUserProfileJ(id, profile)
        }
    }

    // override UserProfileService @JvmBlockingBridge
    @JvmName("doQueryUserProfile")
    public fun doQueryUserProfileJ(id: Long): UserProfile?

    @JvmName("putUserProfile")
    public fun putUserProfileJ(id: Long, profile: UserProfile)
}

public interface MockUserProfileBuilder {
    public fun build(): UserProfile

    public fun nickname(value: String): MockUserProfileBuilder
    public fun email(value: String): MockUserProfileBuilder
    public fun age(value: Int): MockUserProfileBuilder
    public fun qLevel(value: Int): MockUserProfileBuilder
    public fun sex(value: UserProfile.Sex): MockUserProfileBuilder
    public fun sign(value: String): MockUserProfileBuilder

    public companion object {
        @JvmStatic
        @JvmName("builder")
        public operator fun invoke(): MockUserProfileBuilder = MockUPBuilderImpl()
    }
}

public inline fun buildUserProfile(block: MockUserProfileBuilder.() -> Unit): UserProfile {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return MockUserProfileBuilder().apply(block).build()
}

internal class MockUPBuilderImpl : MockUserProfileBuilder, UserProfile {
    override var nickname: String = ""
    override var email: String = ""
    override var age: Int = -1
    override var qLevel: Int = -1
    override var sex: UserProfile.Sex = UserProfile.Sex.UNKNOWN
    override var sign: String = ""

    // unmodifiable
    override fun build(): UserProfile {
        return object : UserProfile by this {}
    }

    override fun nickname(value: String): MockUserProfileBuilder = apply {
        nickname = value
    }

    override fun email(value: String): MockUserProfileBuilder = apply {
        email = value
    }

    override fun age(value: Int): MockUserProfileBuilder = apply {
        age = value
    }

    override fun qLevel(value: Int): MockUserProfileBuilder = apply {
        qLevel = value
    }

    override fun sex(value: UserProfile.Sex): MockUserProfileBuilder = apply {
        sex = value
    }

    override fun sign(value: String): MockUserProfileBuilder = apply {
        sign = value
    }

}

internal class UserProfileServiceImpl : UserProfileService {
    val db = ConcurrentHashMap<Long, UserProfile>()
    val def = buildUserProfile {
    }

    override suspend fun doQueryUserProfile(id: Long): UserProfile {
        return db[id] ?: def
    }

    override suspend fun putUserProfile(id: Long, profile: UserProfile) {
        db[id] = profile
    }

}
