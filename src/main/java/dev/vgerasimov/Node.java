package dev.vgerasimov;

import java.util.Map;
import java.util.Set;

public record Node(String id, Set<String> labels, Map<String, String> properties) {
}
