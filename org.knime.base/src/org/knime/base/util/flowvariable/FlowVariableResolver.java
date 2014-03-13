package org.knime.base.util.flowvariable;

import java.util.NoSuchElementException;

import org.knime.core.node.workflow.FlowVariable;

/**
 * Used to parse the a script containing flow and workflow variables.
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 *
 * @since 2.10
 */
public final class FlowVariableResolver {
    private FlowVariableResolver() {
        // empty
    }

    /**
     * Parses the given text and replaces all variable placeholders by their actual value.
     *
     * @param text the text to parse (for instance an R script).
     * @param model delegator to to retrieve variables
     * @return the changed text with placeholders replaced by variables.
     * @throws NoSuchElementException If a variable cannot be resolved
     */
    public static String parse(final String text, final FlowVariableProvider model) {
        return parse(text, model, FlowVariableEscaper.DEFAULT_INSTANCE);
    }

    /**
     * Parses the given text and replaces all variable placeholders by their actual value.
     *
     * @param text the text to parse (for instance an R script).
     * @param model delegator to to retrieve variables
     * @return the changed text with placeholders replaced by variables.
     * @param escaper A non-null escaper object (possibly sub-class to escape strings etc.)
     * @throws NoSuchElementException If a variable cannot be resolved
     */
    public static String parse(final String text, final FlowVariableProvider model,
        final FlowVariableEscaper escaper) {
        String command = new String(text);
        int currentIndex = 0;
        do {
            currentIndex = command.indexOf("$${", currentIndex);
            if (currentIndex < 0) {
                break;
            }
            int endIndex = command.indexOf("}$$", currentIndex);
            if (endIndex < 0) {
                String badVarName;
                if (command.length() - currentIndex > 20) {
                    badVarName = command.substring(currentIndex, currentIndex + 19) + "...";
                } else {
                    badVarName = command.substring(currentIndex);
                }
                throw new IllegalArgumentException("Variable identifier \"" + badVarName + "\" is not closed");
            }
            String var = command.substring(currentIndex + 4, endIndex);
            switch (command.charAt(currentIndex + 3)) {
                case 'I':
                    String s = escaper.readInt(model, var);
                    command = command.replace("$${I" + var + "}$$", s);
                    break;
                case 'D':
                    s = escaper.readDouble(model, var);
                    command = command.replace("$${D" + var + "}$$", s);
                    break;
                case 'S':
                    s = escaper.readString(model, var);
                    command = command.replace("$${S" + var + "}$$", s);
                    break;
                default:
                    String badVarName;
                    if (endIndex > currentIndex) {
                        badVarName = command.substring(currentIndex, endIndex + "}$$".length());
                    } else if (command.indexOf("\n", currentIndex) >= 0) {
                        badVarName = command.substring(currentIndex, command.indexOf("\n", currentIndex)) + "...";
                    } else if (command.length() - currentIndex > 20) {
                        badVarName = command.substring(currentIndex, currentIndex + 20) + "...";
                    } else {
                        badVarName = command.substring(currentIndex);
                    }
                    throw new IllegalArgumentException("Invalid flow variable identifier \"" + badVarName + "\"; "
                        + "it should start with a type identifer.");
            }
        } while (true);
        return command;
    }

    /**
     * Replaces and returns the given flow variable.
     *
     * @param var flow variable to be extended
     * @return the new variable as string with pre- and suffix for INTEGER, DOUBLE and STRING types
     */
    public static String getPlaceHolderForVariable(final FlowVariable var) {
        switch (var.getType()) {
            case INTEGER:
                return "$${I" + var.getName() + "}$$";
            case DOUBLE:
                return "$${D" + var.getName() + "}$$";
            case STRING:
                return "$${S" + var.getName() + "}$$";
            default:
                throw new RuntimeException("Unsupported flow variable type '" + var.getType() + "'");
        }
    }

    /** Reads the actual value from the {@link FlowVariableResolver} and returns their string representation.
     * This class can be sub-classed when it becomes necessary to escape certain values, e.g. to put a string always
     * in quotes. */
    public static class FlowVariableEscaper {

        /** Default instance that does no escaping, just reads the value. */
        public static final FlowVariableEscaper DEFAULT_INSTANCE = new FlowVariableEscaper();

        /** Create new escaper (only to be used by sub-classes). */
        protected FlowVariableEscaper() {
        }

        /** Read a double from {@link FlowVariableProvider#peekFlowVariableDouble(String)} and return its string.
         * @param model to read from
         * @param var The name of the variable.
         * @return The string value
         * @throws NullPointerException If either argument is null
         * @throws NoSuchElementException If a variable with that name is not defined.
         */
        public String readDouble(final FlowVariableProvider model, final String var) {
            final double d = model.peekFlowVariableDouble(var);
            return Double.toString(d);
        }

        /** Read an integer from {@link FlowVariableProvider#peekFlowVariableInt(String)} and return its string.
         * @param model to read from
         * @param var The name of the variable.
         * @return The string value
         * @throws NullPointerException If either argument is null
         * @throws NoSuchElementException If a variable with that name is not defined.
         */
        public String readInt(final FlowVariableProvider model, final String var) {
            final int i = model.peekFlowVariableInt(var);
            return Integer.toString(i);
        }

        /** Read a string from {@link FlowVariableProvider#peekFlowVariableString(String)} and return it.
         * @param model to read from
         * @param var The name of the variable.
         * @return The string value
         * @throws NullPointerException If either argument is null
         * @throws NoSuchElementException If a variable with that name is not defined.
         */
        public String readString(final FlowVariableProvider model, final String var) {
            return model.peekFlowVariableString(var);
        }
    }
}