package org.h2.test.unit;

import org.h2.message.DbException;
import org.h2.test.TestBase;
import org.h2.util.FuzzyMatchUtils;

public class TestFuzzyMatchUtils extends TestBase {

    public static void main(String... args) throws Exception {
        TestBase.createCaller().init().testFromMain();
    }

    @Override
    public void test() throws Exception {
        testBasicMatches();
        testEmptyStrings();
        testUnicode();
        testSymmetry();
        testInvalidThreshold();
    }

    private void testBasicMatches() {
        check(true, "hello", "hello", 0);
        check(true, "HELLO", "hello", 0);

        check(true, "cat", "cut", 1);
        check(false, "cat", "cut", 0);

        check(true, "cat", "cart", 1);
        check(true, "cart", "cat", 1);

        check(true, "kitten", "sitting", 3);
        check(false, "kitten", "sitting", 2);

        check(false, "ab", "ba", 1);
        check(true, "ab", "ba", 2);

        check(true, "abc", "xyz", Integer.MAX_VALUE);
    }

    private void testEmptyStrings() {
        check(true, "", "", 0);

        check(true, "", "abc", 3);
        check(false, "", "abc", 2);

        check(true, "abc", "", 3);
        check(false, "abc", "", 2);
    }

    private void testUnicode() {
        // One supplementary Unicode code point.
        check(true, "\uD83D\uDE00", "", 1);
        check(false, "\uD83D\uDE00", "", 0);

        // Root-locale lowercasing.
        check(true, "CAF\u00C9", "caf\u00E9", 0);

        // No accent removal.
        check(false, "\u00E9", "e", 0);

        // No Unicode normalization.
        check(false, "\u00E9", "e\u0301", 0);
    }

    private void testSymmetry() {
        String[][] pairs = {
                { "cat", "cart" },
                { "kitten", "sitting" },
                { "", "abc" },
                { "abcdef", "abqdef" }
        };

        for (String[] pair : pairs) {
            for (int threshold = 0; threshold <= 4; threshold++) {
                boolean forward = FuzzyMatchUtils.matches(
                        pair[0], pair[1], threshold);
                boolean backward = FuzzyMatchUtils.matches(
                        pair[1], pair[0], threshold);

                if (forward != backward) {
                    throw new AssertionError(
                            "Symmetry failed: " + pair[0] + ", "
                                    + pair[1] + ", threshold=" + threshold);
                }
            }
        }
    }

    private void testInvalidThreshold() {
        try {
            FuzzyMatchUtils.matches("abc", "abc", -1);
        } catch (DbException expected) {
            return;
        }

        throw new AssertionError(
                "Negative threshold should throw DbException");
    }

    private void check(boolean expected, String left,
            String right, int threshold) {
        boolean actual = FuzzyMatchUtils.matches(
                left, right, threshold);

        if (actual != expected) {
            throw new AssertionError(
                    "left=[" + left + "], right=[" + right
                            + "], threshold=" + threshold
                            + ", expected=" + expected
                            + ", actual=" + actual);
        }
    }

}
