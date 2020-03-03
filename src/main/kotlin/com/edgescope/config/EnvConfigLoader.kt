package com.edgescope.config

import mu.KotlinLogging
import org.apache.commons.beanutils.BeanUtils
import java.lang.reflect.Field
import java.util.ArrayList
import java.util.HashMap

fun <T : Any> T.withEnvOverrides(
    prefix: String,
    envVars: Map<String, String?>? = null,
    defaults: (T.() -> Unit)? = null
): T {
    val config = this

    defaults?.invoke(config)
    return if (envVars != null) {
        EnvConfigLoader.overrideFromEnvironment(config, prefix, envVars)
    } else
        EnvConfigLoader.overrideFromEnvironment(config, prefix)
}

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class RequiresOverride

object EnvConfigLoader {
    private val log = KotlinLogging.logger {}
    private const val ENV_OVERRIDE_VALIDATION_ENABLED = "envOverride.validationEnabled"

    /**
     * Given a class, returns a new instance of that class with overridden properties from environment variables with the given prefix.
     *
     * @param originalObject    an object, presumably used for configuration.
     * @param environmentPrefix the prefix used to name the overriding values. e.g. MY_APP
     * @return an instance of the same type, with overridden property values.
     */
    fun <T : Any> overrideFromEnvironment(originalObject: T, environmentPrefix: String): T {
        val envVars = System.getenv()
        return overrideFromEnvironment(originalObject, environmentPrefix, envVars)
    }

    fun <T : Any> overrideFromEnvironment(
        originalObject: T,
        environmentPrefix: String,
        envVars: Map<String, String?>
    ): T {
        val envOverridesMap = findAllMatchingEnvValues(environmentPrefix, envVars)
        val camelOverridesMap = convertEnvValuesToPropertyNamedValues(environmentPrefix, envOverridesMap)

        return overridePropertiesFromMap(originalObject, camelOverridesMap)
    }

    private fun findAllMatchingEnvValues(
        environmentPrefix: String,
        envVars: Map<String, String?>
    ): Map<String, String?> {

        return envVars.filter {
            it.key.startsWith(environmentPrefix)
        }.toMap()

    }

    private fun convertEnvValuesToPropertyNamedValues(
        environmentPrefix: String,
        envOverridesMap: Map<String, String?>
    ): Map<String, String?> {
        val camelOverridesMap: MutableMap<String, String?> = HashMap()
        for (envKey in envOverridesMap.keys) {
            val upperUnderscorePropertyName = envKey.substring(environmentPrefix.length + 1)
            val camelPropertyName = toLowerCamelCase(upperUnderscorePropertyName)
            camelOverridesMap[camelPropertyName] = envOverridesMap[envKey]
        }
        return camelOverridesMap
    }

    private fun <T : Any> overridePropertiesFromMap(
        originalObject: T,
        camelOverridesMap: Map<String, String?>
    ): T {
        val overriddenObject: T
        val overrideRequiredFields = originalObject.javaClass.declaredFields.filter {
            it.isAnnotationPresent(RequiresOverride::class.java)
        }.toMutableList()

        val overriddenFields: MutableList<Field> = ArrayList()
        return try {

            @Suppress("UNCHECKED_CAST")
            overriddenObject = BeanUtils.cloneBean(originalObject) as T

            for (propertyName in camelOverridesMap.keys) {
                var targetClass: Class<in Any>? = originalObject.javaClass
                var field: Field? = null

                while (field == null) {
                    try {
                        field = targetClass?.getDeclaredField(propertyName)
                    } catch (e: NoSuchFieldException) {
                        targetClass = targetClass?.superclass
                    }

                    if (targetClass == null) {
                        log.warn("Environment override for property $propertyName found, but no matching property exists.")
                        break
                    }

                    if (field == null) {
                        continue
                    }

                    overriddenFields.add(field)
                    BeanUtils.copyProperty(overriddenObject, propertyName, camelOverridesMap[propertyName])
                }
            }

            validateOverrides(overrideRequiredFields, overriddenFields)
            overriddenObject
        } catch (e: Exception) {
            throw RuntimeException(e.message, e)
        }
    }

    private fun validateOverrides(
        overrideRequiredFields: MutableList<Field>,
        overriddenFields: List<Field>
    ) {
        val validationEnabled = System.getProperty(ENV_OVERRIDE_VALIDATION_ENABLED)
        if (validationEnabled != null && validationEnabled.equals("false", ignoreCase = true)) {
            log.info("RequiresOverride validation disabled.")
            return
        }

        if (!overriddenFields.containsAll(overrideRequiredFields)) {
            overrideRequiredFields.removeAll(overriddenFields)
            val missingNames = overrideRequiredFields.joinToString(", ") { obj: Field -> obj.name }

            val message = "Missing required overridden properties: " + missingNames +
                    "\nIf this is a dev environment, you can disable validation by setting system property " +
                    ENV_OVERRIDE_VALIDATION_ENABLED + "=false"
            throw IllegalStateException(message)
        }
    }

    private fun toLowerCamelCase(upperUnderscoreString: String): String {
        val lowerInput = upperUnderscoreString.toLowerCase()
        val stringBuilder = StringBuilder(lowerInput.length)
        val underscore = '_'
        var index = 0
        while (index < lowerInput.length) {
            val c = lowerInput[index]
            if (c == underscore && index > 0) {
                index++
                stringBuilder.append(lowerInput[index].toString().toUpperCase())
            } else {
                stringBuilder.append(c)
            }
            index++
        }
        return stringBuilder.toString()
    }
}