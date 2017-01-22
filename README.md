# env-override

Utility library to override an object's properties with environment variable values.  

[![Build Status](https://travis-ci.org/dtanner/env-override.svg?branch=master)](https://travis-ci.org/dtanner/env-override)

## Main Purpose
Let you define your application's configuration in a **typed** configuration, 
and allow its settings to be overridden by environment variables.

There are a dozen ways to configure your application, and configuration management is often rife confusion, rot, and bugs.  

The approach this tool takes is toward the https://12factor.net/config
technique, with the added benefit of using a typed configuration object, which lets you manage your configuration like code. 

The main class/method is `EnvConfigLoader.overrideFromEnvironment(T config, String environmentPrefix)`
where config is some object you've created, used for storing your config settings. 

## Example Usage
See the EnvConfigLoaderSpec and TestAppConfig for more thorough examples, but here's the idea:

Given an object that you used to store your configuration settings, with some local dev/testing defaults:
```java
public class AppConfig {
    String hostName = "test.foo.com";
    String port = 80;
}
```

Choose a prefix for your environment-specific overrides.  e.g.: 
`export FOO_HOST_NAME="foo.com"`

Then wherever you initialize your app's startup configuration, do something like this:
```
AppConfig appConfig = EnvConfigLoader.overrideFromEnvironment(new AppConfig(), "FOO") 
```

The AppConfig instance will end up with a hostName of `foo.com` and port of `80`.  
i.e. It will have modified the hostName, and left the port with the original value.

## Requirements, Behaviors, Limitations
- The method will **not** mutate your original object. It will return a new object with overridden properties.
- Your property names must strictly match camelCase naming structure.
- It currently supports a flat set of properties. i.e. It doesn't support nested objects or Lists in configuration.  

## Dependencies
- commons-beanutils
- logback-classic

## Issues / Questions
Please open an issue and let me know if you think something's missing, confusing, or broken.   

