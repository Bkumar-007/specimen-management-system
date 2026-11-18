package com.specimen.sms.application;

import static com.specimen.sms.domain.SpecimenStatus.CANCELLED;
import static com.specimen.sms.domain.SpecimenStatus.COLLECTED;
import static com.specimen.sms.domain.SpecimenStatus.IN_TESTING;
import static com.specimen.sms.domain.SpecimenStatus.ORDERED;
import static com.specimen.sms.domain.SpecimenStatus.RECEIVED;
import static com.specimen.sms.domain.SpecimenStatus.REJECTED;
import static com.specimen.sms.domain.SpecimenStatus.REPORTED;
import static com.specimen.sms.domain.SpecimenStatus.RESULTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.specimen.sms.application.common.BusinessRuleException;
import com.specimen.sms.application.specimen.StatusTransitionPolicy;
import org.junit.jupiter.api.Test;

class StatusTransitionPolicyTest {

    @Test
    void happyPathIsAllowed() {
        assertOk(COLLECTED, RECEIVED, false);
        assertOk(RECEIVED, IN_TESTING, false);
        assertOk(IN_TESTING, RESULTED, true);
        assertOk(RESULTED, REPORTED, true);
    }

    @Test
    void cannotSkipSteps() {
        assertIllegal(ORDERED, RESULTED, true);
        assertIllegal(COLLECTED, RESULTED, true);
        assertIllegal(COLLECTED, IN_TESTING, false);
        assertIllegal(RECEIVED, REPORTED, true);
    }

    @Test
    void cannotMoveBackwards() {
        assertIllegal(IN_TESTING, RECEIVED, false);
        assertIllegal(RESULTED, IN_TESTING, true);
    }

    @Test
    void terminalStatesAreFinal() {
        assertIllegal(CANCELLED, RECEIVED, false);
        assertIllegal(REJECTED, IN_TESTING, false);
        assertThatThrownBy(() -> StatusTransitionPolicy.validate(REPORTED, CANCELLED, true, true, false, "oops"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resultedAndReportedNeedResults() {
        assertThatThrownBy(() -> StatusTransitionPolicy.validate(IN_TESTING, RESULTED, false, false, false, null))
                .hasMessageContaining("result");
    }

    @Test
    void cancelAndRejectAreAdminOnlyAndNeedReason() {
        assertThatThrownBy(() -> StatusTransitionPolicy.validate(RECEIVED, REJECTED, false, false, false, "hemolyzed"))
                .hasMessageContaining("admin");
        assertThatThrownBy(() -> StatusTransitionPolicy.validate(RECEIVED, REJECTED, false, true, false, " "))
                .hasMessageContaining("reason");
        assertThatCode(() -> StatusTransitionPolicy.validate(RECEIVED, REJECTED, false, true, false, "hemolyzed"))
                .doesNotThrowAnyException();
    }

    @Test
    void adminCanOverrideReportedWithoutResults() {
        assertThatCode(() -> StatusTransitionPolicy.validate(IN_TESTING, REPORTED, false, true, true, "client request"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> StatusTransitionPolicy.validate(IN_TESTING, REPORTED, false, false, true, "x"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> StatusTransitionPolicy.validate(IN_TESTING, REPORTED, false, true, true, null))
                .hasMessageContaining("reason");
    }

    @Test
    void resultEntryOnlyFromReceivedOrInTesting() {
        assertThatCode(() -> StatusTransitionPolicy.validateResultEntry(RECEIVED)).doesNotThrowAnyException();
        assertThatCode(() -> StatusTransitionPolicy.validateResultEntry(IN_TESTING)).doesNotThrowAnyException();
        assertThatThrownBy(() -> StatusTransitionPolicy.validateResultEntry(COLLECTED))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> StatusTransitionPolicy.validateResultEntry(RESULTED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void allowedNextListsAdminActionsOnlyForAdmins() {
        assertThat(StatusTransitionPolicy.allowedNext(RECEIVED, false)).containsExactly(IN_TESTING);
        assertThat(StatusTransitionPolicy.allowedNext(RECEIVED, true)).contains(IN_TESTING, CANCELLED, REJECTED);
        assertThat(StatusTransitionPolicy.allowedNext(REJECTED, true)).isEmpty();
    }

    private static void assertOk(com.specimen.sms.domain.SpecimenStatus from,
                                 com.specimen.sms.domain.SpecimenStatus to, boolean hasResults) {
        assertThatCode(() -> StatusTransitionPolicy.validate(from, to, hasResults, false, false, null))
                .doesNotThrowAnyException();
    }

    private static void assertIllegal(com.specimen.sms.domain.SpecimenStatus from,
                                      com.specimen.sms.domain.SpecimenStatus to, boolean hasResults) {
        assertThatThrownBy(() -> StatusTransitionPolicy.validate(from, to, hasResults, true, false, "r"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
