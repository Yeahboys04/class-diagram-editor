package com.diagramme.model.enums;

import lombok.Getter;

/**
 * Visibilité des éléments dans un diagramme UML
 */
@Getter
public enum Visibility {
    PUBLIC("+", "public"),
    PRIVATE("-", "private"),
    PROTECTED("#", "protected"),
    PACKAGE("~", "package");

    private final String symbol;
    private final String keyword;

    Visibility(String symbol, String keyword) {
        this.symbol = symbol;
        this.keyword = keyword;
    }

}