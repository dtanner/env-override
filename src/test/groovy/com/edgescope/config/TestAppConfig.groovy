package com.edgescope.config

import groovy.transform.AutoClone

@AutoClone
class TestAppConfig {

    String untouchedValue = "untouched"
    String stringValue = "test"
    int intValue = 1
    Integer integerValue = 1
    long longValue = 1 as long
    char charValue = 'a'
    double doubleValue = 1.3
    BigDecimal bigDecimalValue = 1.0

}
