package ml.alternet.parser.util;

import ml.alternet.parser.Handler;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.scan.Scanner;

/**
 * Embeds the parsing logic on behalf of a kind of rule.
 *
 * @author Philippe Poulard
 *
 * @param <T> The kind of rule.
 */
public interface Parser<T extends Rule>  {

    /**
     * Parse an input with a rule.
     *
     * @param rule The rule.
     * @param scanner The input.
     * @param handler The receiver.
     *
     * @return Indicates whether the rule matched or not.
     */
    Match parse(T rule, Scanner scanner, Handler handler);

    /**
     * Hold the parsing status.
     *
     * <table border="1" summary="Parsing status" style="padding-before: 12px">
     *     <tr><td></td><td>fail ?</td><td>empty ?</td><td>as Optional</td><td>as Mandatory</td></tr>
     *     <tr><th>FAIL</th><td>true</td><td>true</td><td>EMPTY</td><td>FAIL</td></tr>
     *     <tr><th>SUCCESS</th><td>false</td><td>false</td><td>SUCCESS</td><td>SUCCESS</td></tr>
     *     <tr><th>EMPTY</th><td>false</td><td>true</td><td>EMPTY</td><td>FAIL</td></tr>
     * </table>
     *
     * @author Philippe Poulard
     */
    public enum Match {

        /**
         * When a rule Fails, this Match is Empty
         */
        FAIL {
            @Override
            public boolean fail() {
                return true;
            }
            @Override
            public Match asOptional() {
                return EMPTY;
            }
        },

        /**
         * When a rule is a Success, this Match is NOT Empty
         */
        SUCCESS {
            @Override
            public boolean empty() {
                return false;
            }
        },

        /**
         * Not Failed but Empty is a useful status to avoid infinite
         * loops in repeatable rules, but that don't have to be reported
         * as a failure (since after the last hypothetic match we would
         * get a failure).
         */
        EMPTY {
            @Override
            public Match asMandatory() {
                return FAIL;
            }
        };

        /**
         * When a rule or token didn't match the input.
         *
         * @return <code>true</code> for FAIL,
         *      <code>false</code> for EMPTY or SUCCESS.
         */
        public boolean fail() {
            return false;
        };

        /**
         * An optional rule or token that didn't match the input
         * should be turned to EMPTY.
         *
         * @return <code>true</code> for FAIL or EMPTY,
         *      <code>false</code> for SUCCESS.
         */
        public boolean empty() {
            return true;
        }

        /**
         * Turn this match to an optional match,
         * that is to say an optional won't fail
         * but is empty, which allow repeatable
         * rules to avoid looping.
         *
         * @return SUCCES or EMPTY, never FAIL.
         */
        public Match asOptional() {
            return this;
        }

        /**
         * Turn this match to a mandatory match,
         * that is to say a mandatory is never
         * empty.
         *
         * @return SUCCES or FAIL, never EMPTY.
         */
        public Match asMandatory() {
            return this;
        }

    }

    /**
     * Allow to skip tokens around a parser.
     *
     * @author Philippe Poulard
     */
    @SuppressWarnings("rawtypes")
    class Skip implements Parser {

        /**
         * Allow to skip tokens around a parser.
         *
         * @author Philippe Poulard
         */
        public static class SkipRule {

            /**
             * Allow to skip tokens while parsing.
             *
             * @param skipRule The rule to apply.
             * @param before {@code true} for skipping tokens before, {@code false} otherwise.
             * @param after {@code true} for skipping tokens after, {@code false} otherwise.
             */
            public SkipRule(Rule skipRule, boolean before, boolean after) {
                this.skipRule = skipRule;
                this.before = before;
                this.after = after;
            }

            /**
             *
             */
            Rule skipRule;

            /**
             * Skip the characters before the token.
             */
            boolean before;

            /**
             * Skip the characters after the token.
             */
            boolean after;

        }

        SkipRule skipRule;
        Parser parser;

        /**
         * Allow to skip tokens while parsing.
         *
         * @param skipRule The rule to apply.
         * @param parser The parser to wrap.
         * @param before {@code true} for skipping tokens before, {@code false} otherwise.
         * @param after {@code true} for skipping tokens after, {@code false} otherwise.
         */
        public Skip(Rule skipRule, Parser parser, boolean before, boolean after) {
            this.parser = parser;
            this.skipRule = new SkipRule(skipRule, before, after);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Match parse(Rule rule, Scanner scanner, Handler handler) {
            scanner.mark();
            if (skipRule.before) {
                skipRule.skipRule.parse(scanner, Handler.NULL_HANDLER);
            }
            Match match = parser.parse(rule, scanner, handler);
            if (skipRule.after && ! match.fail()) { // fail will cancel
                skipRule.skipRule.parse(scanner, Handler.NULL_HANDLER);
            }
            scanner.commit(! match.fail());
            return match;
        }

    }

}
