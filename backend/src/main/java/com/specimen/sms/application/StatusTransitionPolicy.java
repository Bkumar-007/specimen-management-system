package com.specimen.sms.application;

import static com.specimen.sms.domain.SpecimenStatus.CANCELLED;
import static com.specimen.sms.domain.SpecimenStatus.COLLECTED;
import static com.specimen.sms.domain.SpecimenStatus.IN_TESTING;
import static com.specimen.sms.domain.SpecimenStatus.ORDERED;
import static com.specimen.sms.domain.SpecimenStatus.RECEIVED;
import static com.specimen.sms.domain.SpecimenStatus.REJECTED;
import static com.specimen.sms.domain.SpecimenStatus.REPORTED;
import static com.specimen.sms.domain.SpecimenStatus.RESULTED;

import com.specimen.sms.domain.SpecimenStatus;
import java.util.EnumSet;
import java.util.Set;

/**
 * Pure rules for specimen status changes (no persistence), so they are easy to unit test.
 *
 * <ul>
 *   <li>Normal path: ORDERED, COLLECTED, RECEIVED, IN_TESTING, RESULTED, REPORTED (no skipping).</li>
 *   <li>RESULTED is reached by entering results (from RECEIVED or IN_TESTING) or by PATCH when results exist.</li>
 *   <li>REPORTED requires at least one result; a LAB_ADMIN may override with a reason from RECEIVED or IN_TESTING.</li>
 *   <li>CANCELLED / REJECTED are admin-only, need a reason, and are terminal. REPORTED is also final.</li>
 * </ul>
 */
public final class StatusTransitionPolicy {

    private StatusTransitionPolicy() {
    }

    private static Set<SpecimenStatus> forward(SpecimenStatus from) {
        return switch (from) {
            case ORDERED -> EnumSet.of(COLLECTED);
            case COLLECTED -> EnumSet.of(RECEIVED);
            case RECEIVED -> EnumSet.of(IN_TESTING);
            case IN_TESTING -> EnumSet.of(RESULTED);
            case RESULTED -> EnumSet.of(REPORTED);
            case REPORTED, CANCELLED, REJECTED -> EnumSet.noneOf(SpecimenStatus.class);
        };
    }

    /** Statuses that may be requested next, as shown to the given kind of user. */
    public static Set<SpecimenStatus> allowedNext(SpecimenStatus from, boolean admin) {
        Set<SpecimenStatus> next = EnumSet.noneOf(SpecimenStatus.class);
        next.addAll(forward(from));
        if (admin && !from.isTerminal()) {
            next.add(CANCELLED);
            next.add(REJECTED);
        }
        return next;
    }

    /**
     * Validates a manual status change; throws {@link BusinessRuleException} when illegal.
     *
     * @param hasResults whether the specimen already has at least one result
     * @param override   admin override allowing REPORTED without results
     */
    public static void validate(
            SpecimenStatus from,
            SpecimenStatus to,
            boolean hasResults,
            boolean admin,
            boolean override,
            String reason) {
        if (from == to) {
            throw new BusinessRuleException("Specimen is already " + from);
        }
        if (from.isTerminal()) {
            throw new BusinessRuleException("Specimen is " + from + " and cannot change status");
        }
        if (to == CANCELLED || to == REJECTED) {
            if (!admin) {
                throw new BusinessRuleException("Only a lab admin can set status " + to);
            }
            if (reason == null || reason.isBlank()) {
                throw new BusinessRuleException("A reason is required to set status " + to);
            }
            return;
        }
        if (to == REPORTED && override) {
            if (!admin) {
                throw new BusinessRuleException("Only a lab admin can override the results requirement");
            }
            if (reason == null || reason.isBlank()) {
                throw new BusinessRuleException("A reason is required for an admin override");
            }
            if (from != RECEIVED && from != IN_TESTING) {
                throw new BusinessRuleException("Override applies only from RECEIVED or IN_TESTING");
            }
            return;
        }
        if (!forward(from).contains(to)) {
            throw new BusinessRuleException("Illegal status change " + from + " -> " + to
                    + (forward(from).isEmpty() ? "" : " (next allowed: " + forward(from) + ")"));
        }
        if ((to == RESULTED || to == REPORTED) && !hasResults) {
            throw new BusinessRuleException("At least one result is required before " + to);
        }
    }

    /** Result entry is only allowed from RECEIVED or IN_TESTING. */
    public static void validateResultEntry(SpecimenStatus from) {
        if (!from.acceptsResults()) {
            throw new BusinessRuleException(
                    "Results can only be entered when the specimen is RECEIVED or IN_TESTING (currently " + from + ")");
        }
    }
}
