package com.fluxtah.askplugin.koder.search

import com.fluxtah.askpluginsdk.io.getCurrentWorkingDirectory
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.DirectoryWatcher
import java.io.File
import java.nio.file.Paths

val watcher = DirectoryWatcher.builder()
    .path(Paths.get(File(getCurrentWorkingDirectory()).toURI()))
    .listener(object : DirectoryChangeListener {
        override fun onEvent(event: DirectoryChangeEvent) {
            when (event.eventType()) {
                DirectoryChangeEvent.EventType.CREATE -> {
                    println("File created: ${event.path()}")
                }

                DirectoryChangeEvent.EventType.MODIFY -> {
                    println("File modified: ${event.path()}")
                }

                DirectoryChangeEvent.EventType.DELETE -> {
                    println("File deleted: ${event.path()}")
                }

                DirectoryChangeEvent.EventType.OVERFLOW -> {
                    println("Overflow")
                }
            }
        }
    })
    .build()