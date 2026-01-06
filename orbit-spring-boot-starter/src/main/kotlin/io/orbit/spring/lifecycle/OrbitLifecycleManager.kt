package io.orbit.spring.lifecycle

import io.orbit.core.Orbit
import kotlinx.coroutines.runBlocking
import org.springframework.context.SmartLifecycle

open class OrbitLifecycleManager(
    private val orbit: Orbit,
    private val autoStartup: Boolean,
) : SmartLifecycle {
    override fun start() {
        runBlocking {
            orbit.connect()
        }
    }

    override fun stop() {
        runBlocking {
            orbit.disconnect()
        }
    }

    override fun isRunning(): Boolean =
        runBlocking {
            orbit.isConnected()
        }

    override fun isAutoStartup(): Boolean = autoStartup

    override fun getPhase(): Int = Integer.MAX_VALUE - 1000
}
