package com.edgescope.config

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test
import java.math.BigDecimal

data class RequiresOverrideAppConfig(
    var untouchedValue: String = "untouched",
    @RequiresOverride
    var stringValue: String = "test",
    @RequiresOverride
    var intValue: Int = 1
)

data class TestAppConfig(
    var untouchedValue: String = "untouched",
    var stringValue: String = "test",
    var intValue: Int = 1,
    var longValue: Long = 1L,
    var charValue: Char = 'a',
    var doubleValue: Double = 1.3,
    var bigDecimalValue: BigDecimal = BigDecimal(1.0)
)

open class ParentConfig {
    var stringValue: String = "a"
}

class ChildConfig(var intValue: Int = 1) : ParentConfig()

class EnvConfigLoaderTest {

    @Test
    fun `non-matching env vars should not override properties`() {
        var appConfig = TestAppConfig()
        val envVars = mapOf("JUNK_STRING_VALUE" to "b")

        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, "TEST", envVars)

        appConfig.untouchedValue shouldBe "untouched"
    }

    @Test
    fun `property overrides`() {
        var appConfig = TestAppConfig()

        val envVars = mapOf(
            "TEST_STRING_VALUE" to "b",
            "TEST_INT_VALUE" to "2",
            "TEST_LONG_VALUE" to "2",
            "TEST_CHAR_VALUE" to "b",
            "TEST_DOUBLE_VALUE" to "2.0",
            "TEST_BIG_DECIMAL_VALUE" to "2.0"
        )

        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, "TEST", envVars)
        appConfig.stringValue shouldBe "b"
        appConfig.intValue shouldBe 2
        appConfig.longValue shouldBe 2
        appConfig.charValue shouldBe 'b'
        appConfig.doubleValue shouldBe 2.0
        appConfig.bigDecimalValue shouldBe BigDecimal("2.0")
    }

    @Test
    fun `extra environment values don't cause an error`() {
        var appConfig = TestAppConfig()
        val envVars = mapOf("TEST_UNKNOWN_VALUE" to "a")

        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, "TEST", envVars)

        appConfig.untouchedValue shouldBe "untouched"
    }

    @Test
    fun `not overriding RequiresOverride fields will throw an exception`() {
        val appConfig = RequiresOverrideAppConfig()
        val envVars = mapOf("TEST_UNKNOWN_VALUE" to "a")

        val exception = shouldThrow<RuntimeException> {
            EnvConfigLoader.overrideFromEnvironment(appConfig, "TEST", envVars)
        }
        exception.message shouldBe "Missing required overridden properties: stringValue, intValue\n" +
                "If this is a dev environment, you can disable validation by setting system property envOverride.validationEnabled=false"
    }

    @Test
    fun `RequiresOverride validation is disabled when the system property is set`() {
        val appConfig = RequiresOverrideAppConfig()
        val envVars = mapOf("TEST_UNKNOWN_VALUE" to "a")

        System.setProperty("envOverride.validationEnabled", "false")

        EnvConfigLoader.overrideFromEnvironment(appConfig, "TEST", envVars)
    }

    @Test
    fun `overriding a RequiresOverride field will work successfully`() {
        var appConfig = RequiresOverrideAppConfig()
        val envVars = mapOf(
            "TEST_STRING_VALUE" to "modified",
            "TEST_INT_VALUE" to "2"
        )

        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, "TEST", envVars)

        appConfig.stringValue shouldBe "modified"
        appConfig.intValue shouldBe 2
    }

    @Test
    fun `class hierarchy overrides work`() {
        var appConfig = ChildConfig()
        val envVars = mapOf(
            "TEST_STRING_VALUE" to "modified",
            "TEST_INT_VALUE" to "2"
        )

        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, "TEST", envVars)

        appConfig.stringValue shouldBe "modified"
        appConfig.intValue shouldBe 2
    }
}