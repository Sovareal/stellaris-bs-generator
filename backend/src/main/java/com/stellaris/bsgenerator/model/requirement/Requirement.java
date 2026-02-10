package com.stellaris.bsgenerator.model.requirement;

import java.util.List;

/**
 * A single requirement condition within a category block.
 * Conditions within a category are implicitly ANDed.
 */
public sealed interface Requirement {

    /** Require the specified value (e.g., value = ethic_authoritarian). */
    record Value(String value) implements Requirement {}

    /** Forbid the specified value (NOT = { value = X }). */
    record Not(String value) implements Requirement {}

    /** Forbid all listed values (NOR = { value = A  value = B }). */
    record Nor(List<String> values) implements Requirement {}

    /** Require at least one of the listed values (OR = { value = A  value = B }). */
    record Or(List<String> values) implements Requirement {}
}
