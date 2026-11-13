/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 */
package org.h2.util;

import java.util.Locale;

import org.h2.message.DbException;

/**
 * Compares strings by the number of insertions, deletions, and substitutions.
 */
public final class FuzzyMatchUtils {

    private FuzzyMatchUtils() {
    }

    /**
     * Returns whether the strings are within the allowed edit distance.
     * SQL NULL arguments should be handled by the calling function.
     *
     * @param left the first string
     * @param right the second string
     * @param maxDistance the maximum number of edits, at least zero
     * @return whether the strings match within the limit
     */
    public static boolean matches(String left, String right, int maxDistance) {
        if (maxDistance < 0) {
            throw DbException.getInvalidValueException("FUZZY_MATCH threshold", maxDistance);
        }

        left = left.toLowerCase(Locale.ROOT);
        right = right.toLowerCase(Locale.ROOT);
        if (left.equals(right)) {
            return true;
        }

        int leftLength = left.codePointCount(0, left.length());
        int rightLength = right.codePointCount(0, right.length());
        if (Math.abs(leftLength - rightLength) > maxDistance) {
            return false;
        }
        if (maxDistance >= Math.max(leftLength, rightLength)) {
            return true;
        }

        int[] first = left.codePoints().toArray();
        int[] second = right.codePoints().toArray();
        if (second.length > first.length) {
            int[] swap = first;
            first = second;
            second = swap;
        }

        int[] previous = new int[second.length + 1];
        int[] current = new int[second.length + 1];
        int outside = maxDistance + 1;
        for (int j = 0; j < previous.length; j++) {
            previous[j] = Math.min(j, outside);
        }

        for (int i = 1; i <= first.length; i++) {
            int start = Math.max(1, i - maxDistance);
            int end = (int) Math.min(second.length, (long) i + maxDistance);
            current[start - 1] = start == 1 ? Math.min(i, outside) : outside;
            int rowMinimum = outside;

            for (int j = start; j <= end; j++) {
                int cost = first[i - 1] == second[j - 1] ? 0 : 1;
                int distance = Math.min(previous[j] + 1, current[j - 1] + 1);
                distance = Math.min(distance, previous[j - 1] + cost);
                current[j] = distance;
                rowMinimum = Math.min(rowMinimum, distance);
            }
            if (rowMinimum > maxDistance) {
                return false;
            }
            if (end < second.length) {
                current[end + 1] = outside;
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[second.length] <= maxDistance;
    }
}
