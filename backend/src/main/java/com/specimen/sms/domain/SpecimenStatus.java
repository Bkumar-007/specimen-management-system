package com.specimen.sms.domain;

/**
 * Lifecycle of a specimen. ORDERED is the initial state of an order (no specimen yet);
 * physical specimens start at COLLECTED or RECEIVED when accessioned.
 */
public enum SpecimenStatus {
    ORDERED,
    COLLECTED,
    RECEIVED,
    IN_TESTING,
    RESULTED,
    REPORTED,
    CANCELLED,
    REJECTED;

    /** No further transitions are possible once a specimen reaches one of these states. */
    public boolean isTerminal() {
        return this == REPORTED || this == CANCELLED || this == REJECTED;
    }

    /** True if the specimen is in a state where analyte results may be entered. */
    public boolean acceptsResults() {
        return this == RECEIVED || this == IN_TESTING;
    }
}
