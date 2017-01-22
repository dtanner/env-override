package com.edgescope.config;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.getenv;


public class EnvConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(EnvConfigLoader.class);

    /**
     * Given a class, returns a new instance of that class with overridden properties from environment variables with the given prefix.
     *
     * @param originalObject            an object, presumably used for configuration.
     * @param environmentPrefix the prefix used to name the overriding values. e.g. MY_APP
     * @return an instance of the same type, with overridden property values.
     */
    public static <T> T overrideFromEnvironment(T originalObject, String environmentPrefix) {
        Map<String, String> envVars = getenv();
        return overrideFromEnvironment(originalObject, environmentPrefix, envVars);
    }

    @SuppressWarnings("unchecked")
    static <T> T overrideFromEnvironment(T config, String environmentPrefix, Map<String, String> envVars) {
        Map<String, String> envOverridesMap = new HashMap<>();
        for (String envKey : envVars.keySet()) {
            if (envKey.startsWith(environmentPrefix)) {
                envOverridesMap.put(envKey, envVars.get(envKey));
            }
        }

        Map<String, String> camelOverridesMap = new HashMap<>();
        for (String envKey : envOverridesMap.keySet()) {
            String upperUnderscorePropertyName = envKey.substring(environmentPrefix.length() + 1);
            String camelPropertyName = toLowerCamelCase(upperUnderscorePropertyName);
            camelOverridesMap.put(camelPropertyName, envOverridesMap.get(envKey));
        }

        T overriddenObject;
        try {
            overriddenObject = (T) BeanUtils.cloneBean(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        T overriddenObject = cloneObject(config);
        for (String propertyName : camelOverridesMap.keySet()) {
            try {
                BeanUtils.copyProperty(overriddenObject, propertyName, camelOverridesMap.get(propertyName));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            setProperty(overriddenObject, propertyName, camelOverridesMap.get(propertyName));
        }

        return overriddenObject;
    }

    /**
     * Given e.g. FOO or FOO_BAR, results in foo or fooBar
     *
     * @param upperUnderscoreString
     * @return the string in lowerCamelCase
     */
    private static String toLowerCamelCase(String upperUnderscoreString) {
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

    // or clone/copy bean from beanutils?
    private static <T> T cloneObject(T obj) {
        try {
            T clone = (T) obj.getClass().newInstance();
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                field.set(clone, field.get(obj));
            }
            return clone;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // or maybe beanutils.copyProperty if this isn't robust enough
    private static boolean setProperty(Object object, String fieldName, String fieldValueString) {
        Class<?> clazz = object.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Class fieldType = field.getType();
//            if (fieldType.equals(Integer.class) || fieldType.equals(Integer.TYPE)) {
//                field.setInt(object, Integer.parseInt(fieldValueString));
//            } else if (fieldType.equals(Long.class) || fieldType.equals(Long.TYPE)) {
//                field.setLong(object, Long.parseLong(fieldValueString));
//            } else if (fieldType.equals(Boolean.class) || fieldType.equals(Boolean.TYPE)) {
//                field.setBoolean(object, Boolean.parseBoolean(fieldValueString));
//            } else if (fieldType.equals(Long.class) || fieldType.equals(Long.TYPE)) {
//                field.set(object, Long.parseLong(fieldValueString));
//
//            } else {
//
//            }
//            Class.forName(field.getDeclaringClass().getName());
//            field.getDeclaringClass().getConstructor(String.class).newInstance(fieldValueString);

            // todo - this doesn't work for primitives like int
            field.set(object, field.getType().getConstructor(String.class).newInstance(fieldValueString));
            return true;
        } catch (NoSuchFieldException e) {
            log.warn("Environment override for property " + fieldName + " found, but no matching property exists.");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return false;
    }
}
