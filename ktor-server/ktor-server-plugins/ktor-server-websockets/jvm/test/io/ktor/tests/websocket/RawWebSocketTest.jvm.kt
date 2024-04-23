/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.websocket

import kotlinx.coroutines.debug.*

actual fun dumpCoroutines() {
    DebugProbes.dumpCoroutines()
}

actual fun installProbes() {
    DebugProbes.install()
}
