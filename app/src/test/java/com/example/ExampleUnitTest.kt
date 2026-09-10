package com.example

import com.example.util.DeviceHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun formatBytes_correctOutput() {
        assertEquals("500 B", DeviceHelper.formatBytes(500))
        assertEquals("1.0 KB", DeviceHelper.formatBytes(1024))
        assertEquals("1.5 MB", DeviceHelper.formatBytes(1572864))
        assertEquals("1.00 GB", DeviceHelper.formatBytes(1073741824))
    }

    @Test
    fun formatSpeed_correctOutput() {
        assertEquals("500.0 KB/s", DeviceHelper.formatSpeed(512000))
        assertEquals("2.00 MB/s", DeviceHelper.formatSpeed(2097152))
    }
}

