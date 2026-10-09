package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;

import java.util.List;

/** Merged nodes of a directory plus every file that had to be skipped. */
public record LoadResult(ClausewitzNode root, List<ParseFailure> failures) {}
