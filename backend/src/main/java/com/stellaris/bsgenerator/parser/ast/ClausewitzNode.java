package com.stellaris.bsgenerator.parser.ast;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalDouble;

public record ClausewitzNode(String key, String value, List<ClausewitzNode> children) {

    public static ClausewitzNode root(List<ClausewitzNode> children) {
        return new ClausewitzNode(null, null, children);
    }

    public static ClausewitzNode leaf(String key, String value) {
        return new ClausewitzNode(key, value, List.of());
    }

    public static ClausewitzNode block(String key, List<ClausewitzNode> children) {
        return new ClausewitzNode(key, null, children);
    }

    public static ClausewitzNode bareValue(String value) {
        return new ClausewitzNode(null, value, List.of());
    }

    public boolean isLeaf() {
        return value != null && children.isEmpty();
    }

    public boolean isBlock() {
        return value == null && !children.isEmpty();
    }

    public boolean isBareValue() {
        return key == null && value != null;
    }

    public Optional<ClausewitzNode> child(String childKey) {
        return children.stream()
                .filter(n -> childKey.equals(n.key()))
                .findFirst();
    }

    public List<ClausewitzNode> children(String childKey) {
        return children.stream()
                .filter(n -> childKey.equals(n.key()))
                .toList();
    }

    public Optional<String> childValue(String childKey) {
        return child(childKey)
                .map(ClausewitzNode::value);
    }

    public int childInt(String childKey, int defaultValue) {
        return childValue(childKey)
                .map(v -> (int) Double.parseDouble(v))
                .orElse(defaultValue);
    }

    public double childDouble(String childKey, double defaultValue) {
        return childValue(childKey)
                .map(Double::parseDouble)
                .orElse(defaultValue);
    }

    public boolean childBool(String childKey, boolean defaultValue) {
        return childValue(childKey)
                .map(v -> v.equalsIgnoreCase("yes"))
                .orElse(defaultValue);
    }

    public List<String> bareValues() {
        return children.stream()
                .filter(ClausewitzNode::isBareValue)
                .map(ClausewitzNode::value)
                .toList();
    }
}
