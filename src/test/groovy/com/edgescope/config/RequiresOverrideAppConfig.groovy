package com.edgescope.config

import groovy.transform.AutoClone

@AutoClone
class RequiresOverrideAppConfig {

    String untouchedValue = "untouched"

    @RequiresOverride
    String stringValue = "test"

    @RequiresOverride
    int intValue = 1
}
