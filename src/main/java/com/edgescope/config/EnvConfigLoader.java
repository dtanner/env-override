package com.edgescope.config;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EnvConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(EnvConfigLoader.class);

    /**
     * Given a class, returns a new instance of that class with overridden properties from environment variables with the given prefix.
     *
     * @param originalObject    an object, presumably used for configuration.
     * @param environmentPrefix the prefix used to name the overriding values. e.g. MY_APP
     * @return an instance of the same type, with overridden property values.
     */
    public static <T> T overrideFromEnvironment(T originalObject, String environmentPrefix) {
        Map<String, String> envVars = System.getenv();
        return overrideFromEnvironment(originalObject, environmentPrefix, envVars);
    }

    static <T> T overrideFromEnvironment(T originalObject, String environmentPrefix, Map<String, String> envVars) {

        Map<String, String> envOverridesMap = findAllMatchingEnvValues(environmentPrefix, envVars);

        Map<String, String> camelOverridesMap = convertEnvValuesToPropertyNamedValues(environmentPrefix, envOverridesMap);

        T overriddenObject = overridePropertiesFromMap(originalObject, camelOverridesMap);

        return overriddenObject;
    }

    protected static Map<String, String> findAllMatchingEnvValues(String environmentPrefix, Map<String, String> envVars) {
        Map<String, String> envOverridesMap = new HashMap<>();
        for (String envKey : envVars.keySet()) {
            if (envKey.startsWith(environmentPrefix)) {
                envOverridesMap.put(envKey, envVars.get(envKey));
            }
        }
        return envOverridesMap;
    }

    protected static Map<String, String> convertEnvValuesToPropertyNamedValues(String environmentPrefix, Map<String, String> envOverridesMap) {
        Map<String, String> camelOverridesMap = new HashMap<>();
        for (String envKey : envOverridesMap.keySet()) {
            String upperUnderscorePropertyName = envKey.substring(environmentPrefix.length() + 1);
            String camelPropertyName = toLowerCamelCase(upperUnderscorePropertyName);
            camelOverridesMap.put(camelPropertyName, envOverridesMap.get(envKey));
        }
        return camelOverridesMap;
    }

    @SuppressWarnings("unchecked")
    protected static <T> T overridePropertiesFromMap(T originalObject, Map<String, String> camelOverridesMap) {
        T overriddenObject;
        try {
            overriddenObject = (T) BeanUtils.cloneBean(originalObject);
            for (String propertyName : camelOverridesMap.keySet()) {
                try {
                    overriddenObject.getClass().getDeclaredField(propertyName);
                    BeanUtils.copyProperty(overriddenObject, propertyName, camelOverridesMap.get(propertyName));
                } catch (NoSuchFieldException e) {
                    log.warn("Environment override for property " + propertyName + " found, but no matching property exists.");
                }
            }
            return overriddenObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static String toLowerCamelCase(String upperUnderscoreString) {
        String lowerInput = upperUnderscoreString.toLowerCase();

        StringBuilder stringBuilder = new StringBuilder(lowerInput.length());
        char underscore = '_';
        for (int index = 0; index < lowerInput.length(); index++) {
            char c = lowerInput.charAt(index);
            if (c == underscore && index > 0) {
                index++;
                stringBuilder.append(String.valueOf(lowerInput.charAt(index)).toUpperCase());
            } else {
                stringBuilder.append(c);
            }

        }

        return stringBuilder.toString();
    }

}
