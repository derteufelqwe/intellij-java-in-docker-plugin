package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.target.TargetEnvironment

@Suppress("UnstableApiUsage")
class TargetDebuggerConnection(private val remoteConnection: RemoteConnection,
                               val debuggerPortRequest: TargetEnvironment.TargetPortBinding) {
    private var remoteConnectionResolved: Boolean = false

    fun resolveRemoteConnection(environment: TargetEnvironment) {
        val localPort = environment.targetPortBindings[debuggerPortRequest]
        remoteConnection.apply {
            debuggerHostName = "localhost"
            debuggerAddress = localPort.toString()
        }
        remoteConnectionResolved = true
    }

    fun getResolvedRemoteConnection(): RemoteConnection {
        if (!remoteConnectionResolved) {
            throw IllegalStateException("The connection parameters to the debugger must be resolved with the target environment")
        }
        return remoteConnection
    }
}