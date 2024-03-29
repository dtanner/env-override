# env-override

Utility library to override an object's properties with environment variable values.  

![CI](https://github.com/dtanner/env-override/workflows/CI/badge.svg)

# ARCHIVED
This was packaged under `com.edgescope`, a domain I no longer own. This project used to be deployed to bintray, which has been shut down, which makes maven central the only practical repository. But they require control of the group (domain) to allow write access, which is not worth it for me. This code still works fine, but it's dependencies are getting old, and it could use a rewrite anyway to support `val`s instead of `var`s, and not require annotations to get the `@RequiresOverride` feature.


# Usage
Available from the github repository as: `implementation 'com.edgescope:env-override:$version'`.  
[latest release](https://github.com/dtanner/env-override/releases/latest)

In your gradle script, add github as a maven repository:
```groovy
repositories {
    maven("https://maven.pkg.github.com/dtanner/env-override")
}
```

## Main Purpose
Let you define your application's configuration in a **typed** configuration, 
and allow its settings to be overridden by environment variables.

There are a dozen ways to configure your application, and configuration management is often rife with confusion, rot, and bugs.  

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

    export FOO_HOST_NAME="foo.com"

Then wherever you initialize your app's startup configuration, do something like this:

    AppConfig appConfig = EnvConfigLoader.overrideFromEnvironment(new AppConfig(), "FOO") 


The AppConfig instance will end up with a hostName of `foo.com` and port of `80`.  
i.e. It will have modified the hostName, and left the port with the original value.

## RequiresOverride
You can indicate that a configuration field must be overridden using the @RequiresOverride annotation.  
This is useful for fields that you know should be overridden in production, and want to an extra check to ensure it happens.
For example, using the AppConfig example again:
```java
import com.edgescope.config.RequiresOverride;

public class AppConfig {
    @RequiresOverride
    String hostName = "test.foo.com";
    
    String port = 80;
}
```

When the `EnvConfigLoader.overrideFromEnvironment` method is called, it checks that the hostName is overridden.  
If not, it will throw a RuntimeException indicating which fields haven't been overridden.
 
You can disable this validation (e.g. for environments that use all the default values) by setting the `envOverride.validationEnabled` System property to `false`.  It is enabled by default.

## Requirements, Behaviors, Limitations
- The method will **not** mutate your original object. It will return a new object with overridden properties.
- Your property names must strictly match camelCase naming structure.
- It currently supports a flat set of properties. i.e. It doesn't support nested objects or Lists in configuration.  

## Dependencies
- commons-beanutils
- logback-classic

## Issues / Questions
Please open an issue and let me know if you think something's missing, confusing, or broken.   

