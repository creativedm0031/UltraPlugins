package de.creativedm.ultra.ranks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One rank as stored in ranks.yml. Texts (display, prefix, suffix, description) are MiniMessage. */
final class Rank {

    final String id;
    String display;
    String prefix = "";
    String suffix = "";
    int weight;
    String nameColor = "WHITE";
    String icon = "NAME_TAG";
    String description = "";
    /** permission -> granted (true) or negated (false), in the order they were added */
    final Map<String, Boolean> permissions = new LinkedHashMap<>();
    final List<String> parents = new ArrayList<>();
    double price;
    /** "permanent" or a duration like 30d */
    String purchaseDuration = "permanent";

    Rank(String id) {
        this.id = id;
        this.display = id;
    }
}
