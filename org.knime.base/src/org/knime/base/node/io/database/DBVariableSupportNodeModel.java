/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database;

import org.knime.core.node.workflow.FlowVariable;

/**
 * Interface for all database model implementations. Currently only the
 * methods for accessing flow variables are implemented.
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 */
public interface DBVariableSupportNodeModel {

    /**
     * Delegate access to flow variable of type INTEGER.
     * @param name identifier for flow variable
     * @return int value
     */
    public int delegatePeekFlowVariableInt(final String name);

    /**
     * Delegate access to flow variable of type DOUBLE.
     * @param name identifier for flow variable
     * @return double value
     */
    public double delegatePeekFlowVariableDouble(final String name);

    /**
     * Delegate access to flow variable of type STRING.
     * @param name identifier for flow variable
     * @return String value
     */
    public String delegatePeekFlowVariableString(final String name);

    /**
     * Resolver providing methods to parse queries and wrap certain variables.
     */
    public static final class Resolver {
        private Resolver() {
            // empty
        }
        /**
         * Parses the given database query and replaces the variables.
         * @param query SQL query to parse
         * @param model delegator to to retrieve variables
         * @return SQL query where the variables have been replace with
         *         their actual value
         */
        public static String parse(final String query,
                final DBVariableSupportNodeModel model) {
            String command = query;
            int currentIndex = 0;
            do {
                currentIndex = command.indexOf("$${", currentIndex);
                if (currentIndex < 0) {
                    break;
                }
                int endIndex = command.indexOf("}$$", currentIndex);
                String var = command.substring(currentIndex + 4, endIndex);
                switch (command.charAt(currentIndex + 3)) {
                    case 'I' :
                        int i = model.delegatePeekFlowVariableInt(var);
                        command = command.replace(
                                "$${I" + var + "}$$", Integer.toString(i));
                        break;
                    case 'D' :
                        double d = model.delegatePeekFlowVariableDouble(var);
                        command = command.replace(
                                "$${D" + var + "}$$", Double.toString(d));
                        break;
                    case 'S' :
                        String s = model.delegatePeekFlowVariableString(var);
                        command = command.replace("$${S" + var + "}$$", s);
                        break;
                }
            } while (true);
            return command;
        }

        /**
         * Replaces and returns the given flow variable.
         * @param var flow variable to be extended
         * @return the new variable as string with pre- and suffix for
         *         INTEGER, DOUBLE and STRING types
         */
        public static String wrap(final FlowVariable var) {
            switch (var.getType()) {
                case INTEGER :
                    return "$${I" + var.getName() + "}$$";
                case DOUBLE :
                    return "$${D" + var.getName() + "}$$";
                case STRING :
                    return "$${S" + var.getName() + "}$$";
                default : throw new RuntimeException(
                    "Unsupported flow variable type '" + var.getType() + "'");
            }
        }
    }

}
