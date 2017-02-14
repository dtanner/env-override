package com.edgescope.config

import spock.lang.Specification
import spock.lang.Unroll

class EnvConfigLoaderSpec extends Specification {

    def "non-overridden values remain"() {
        given:
        TestAppConfig appConfig = new TestAppConfig()
        Map<String, String> envVars = [JUNK_STRING_VALUE: "b"]

        when:
        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, 'TEST', envVars)

        then:
        appConfig.untouchedValue == "untouched"
    }

    @Unroll
    def "#propertyName type override"() {
        given:
        TestAppConfig appConfig = new TestAppConfig()
        Map<String, String> envVars = environmentMap

        when:
        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, 'TEST', envVars)

        then:
        appConfig.getProperty(propertyName) == expectedValue

        where:
        propertyName      | environmentMap                  | expectedValue
        'stringValue'     | [TEST_STRING_VALUE: "b"]        | 'b'
        'intValue'        | [TEST_INT_VALUE: "2"]           | 2
        'integerValue'    | [TEST_INTEGER_VALUE: "2"]       | 2
        'longValue'       | [TEST_LONG_VALUE: "2"]          | 2
        'charValue'       | [TEST_CHAR_VALUE: "b"]          | 'b'
        'doubleValue'     | [TEST_DOUBLE_VALUE: "2.0"]      | 2.0
        'bigDecimalValue' | [TEST_BIG_DECIMAL_VALUE: "2.0"] | 2.0
    }

    def "extra environment values don't cause an error (e.g. when phasing out properties from the code)"() {
        given:
        TestAppConfig appConfig = new TestAppConfig()
        Map<String, String> envVars = [TEST_UNKNOWN_VALUE: "a"]

        when:
        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, 'TEST', envVars)

        then:
        appConfig.untouchedValue == "untouched"
        noExceptionThrown()
    }

    @Unroll
    def "toLowerCamelCase for #input should output #output"() {
        expect:
        EnvConfigLoader.toLowerCamelCase(input) == output

        where:
        input     | output
        'FOO'     | 'foo'
        'FOO_BAR' | 'fooBar'
    }

    def "not overriding RequiresOverride fields will throw an exception"() {
        given:
        RequiresOverrideAppConfig appConfig = new RequiresOverrideAppConfig()
        Map<String, String> envVars = [TEST_UNKNOWN_VALUE: "a"]

        when:
        EnvConfigLoader.overrideFromEnvironment(appConfig, 'TEST', envVars)

        then:
        RuntimeException ex = thrown()
        ex.message == "Missing required overridden properties: stringValue, intValue"
    }

    def "RequiresOverride validation is disabled when the system property is set"() {
        given:
        RequiresOverrideAppConfig appConfig = new RequiresOverrideAppConfig()
        Map<String, String> envVars = [TEST_UNKNOWN_VALUE: "a"]

        System.setProperty("envOverride.validationEnabled", "false")

        when:
        EnvConfigLoader.overrideFromEnvironment(appConfig, 'TEST', envVars)

        then:
        noExceptionThrown()
    }

    def "overriding a RequiresOverride field will work successfully"() {
        given:
        RequiresOverrideAppConfig appConfig = new RequiresOverrideAppConfig()
        Map<String, String> envVars = [TEST_STRING_VALUE: "a", TEST_INT_VALUE: "2"]

        when:
        appConfig = EnvConfigLoader.overrideFromEnvironment(appConfig, 'TEST', envVars)

        then:
        noExceptionThrown()
        appConfig.stringValue == "a"
    }
}
