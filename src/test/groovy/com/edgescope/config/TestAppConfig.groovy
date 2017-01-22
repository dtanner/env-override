package com.edgescope.config

import groovy.transform.AutoClone

@AutoClone
class TestAppConfig {

    String untouchedValue = "untouched"
    String stringValue = "test"
    int intValue = 1
    BigDecimal bigDecimalValue = 1.0


    // todo test a whole bunch more types

}
