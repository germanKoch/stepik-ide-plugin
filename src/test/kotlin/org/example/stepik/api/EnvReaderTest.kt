package org.example.stepik.api

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvReaderTest {

    private fun withTempDir(block: (String) -> Unit) {
        val dir = File(System.getProperty("java.io.tmpdir"), "env-reader-test-${System.nanoTime()}")
        dir.mkdirs()
        try {
            block(dir.absolutePath)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reads valid env file`() {
        withTempDir { dir ->
            File(dir, ".env").writeText("KEY1=value1\nKEY2=value2\n")
            val env = EnvReader.read(dir)
            assertEquals("value1", env["KEY1"])
            assertEquals("value2", env["KEY2"])
        }
    }

    @Test
    fun `returns empty map for missing file`() {
        withTempDir { dir ->
            assertTrue(EnvReader.read(dir).isEmpty())
        }
    }

    @Test
    fun `skips comments and blank lines`() {
        withTempDir { dir ->
            File(dir, ".env").writeText("# comment\n\nKEY=val\n  # another comment\n")
            val env = EnvReader.read(dir)
            assertEquals(1, env.size)
            assertEquals("val", env["KEY"])
        }
    }

    @Test
    fun `handles values with equals signs`() {
        withTempDir { dir ->
            File(dir, ".env").writeText("KEY=val=ue=extra\n")
            val env = EnvReader.read(dir)
            assertEquals("val=ue=extra", env["KEY"])
        }
    }

    @Test
    fun `skips malformed lines`() {
        withTempDir { dir ->
            File(dir, ".env").writeText("NOEQUALS\n=nokey\nGOOD=value\n")
            val env = EnvReader.read(dir)
            assertEquals(1, env.size)
            assertEquals("value", env["GOOD"])
        }
    }

    @Test
    fun `readCredentials returns pair when both present`() {
        withTempDir { dir ->
            File(dir, ".env").writeText("STEPIK_CLIENT_ID=myid\nSTEPIK_CLIENT_SECRET=mysecret\n")
            val creds = EnvReader.readCredentials(dir)
            assertEquals("myid" to "mysecret", creds)
        }
    }

    @Test
    fun `readCredentials returns null when missing`() {
        withTempDir { dir ->
            File(dir, ".env").writeText("STEPIK_CLIENT_ID=myid\n")
            assertNull(EnvReader.readCredentials(dir))
        }
    }

    @Test
    fun `readCredentials returns null for blank values`() {
        withTempDir { dir ->
            File(dir, ".env").writeText("STEPIK_CLIENT_ID=\nSTEPIK_CLIENT_SECRET=mysecret\n")
            assertNull(EnvReader.readCredentials(dir))
        }
    }
}
