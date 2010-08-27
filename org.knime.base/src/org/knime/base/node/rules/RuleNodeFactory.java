/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.knime.base.node.rules.Rule.Operators;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;

/**
 * This utility class has function to create all kinds of rule nodes.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
final class RuleNodeFactory {
    private RuleNodeFactory() {
    }

    /**
     * Creates a new AND node.
     *
     * @param left the left node
     * @param right the right node
     *
     * @return a new AND node
     */
    public static RuleNode and(final RuleNode left, final RuleNode right) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return left.evaluate(row) && right.evaluate(row);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "(" + left + " " + Operators.AND + " " + right + ")";
            }
        };
    }

    /**
     * Creates a new OR node.
     *
     * @param left the left node
     * @param right the right node
     *
     * @return a new OR node
     */
    public static RuleNode or(final RuleNode left, final RuleNode right) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return left.evaluate(row) || right.evaluate(row);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "(" + left + " " + Operators.OR + " " + right + ")";
            }
        };
    }

    /**
     * Creates a new XOR node.
     *
     * @param left the left node
     * @param right the right node
     *
     * @return a new XOR node
     */
    public static RuleNode xor(final RuleNode left, final RuleNode right) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return left.evaluate(row) ^ right.evaluate(row);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "(" + left + " " + Operators.XOR + " " + right + ")";
            }
        };
    }

    /**
     * Creates a new NOT node.
     *
     * @param node the node to be negated
     *
     * @return a new NOT node
     */
    public static RuleNode not(final RuleNode node) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return !node.evaluate(row);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return Operators.NOT + " " + node;
            }
        };
    }

    /**
     * Creates a new greater than node, that compares the values in two columns.
     *
     * @param leftCol the left column's index
     * @param rightCol the right column's index
     * @param comp the comparator that should be used for comparing the two
     *            columns
     *
     * @return a new greater than node
     */
    public static RuleNode gt(final int leftCol, final int rightCol,
            final DataValueComparator comp) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return comp
                        .compare(row.getCell(leftCol), row.getCell(rightCol)) > 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + leftCol + "$ " + Operators.GT + " $" + rightCol
                        + "$";
            }
        };
    }

    /**
     * Creates a new greater than node, that compares the value in a column to a
     * fixed number.
     *
     * @param col the left column's index
     * @param value the fixed number on the right side
     *
     * @return a new greater than node
     */
    public static RuleNode gt(final int col, final Number value) {
        if (value instanceof Integer) {
            final int v = value.intValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((IntValue)c).getIntValue() > v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.GT + " " + value;
                }
            };
        } else {
            final double v = value.doubleValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((DoubleValue)c).getDoubleValue() > v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.GT + " " + value;
                }
            };
        }
    }

    /**
     * Creates a new greater than node, that compares the value in a column to a
     * fixed string.
     *
     * @param col the left column's index
     * @param value the fixed string on the right side
     *
     * @return a new greater than node
     */
    public static RuleNode gt(final int col, final String value) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c = row.getCell(col);
                if (c.isMissing()) {
                    return false;
                }
                return c.toString().compareTo(value) > 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col + "$ " + Operators.GT + " \"" + value + "\"";
            }
        };
    }

    /**
     * Creates a new greater than or equal node, that compares the values in two
     * columns.
     *
     * @param leftCol the left column's index
     * @param rightCol the right column's index
     * @param comp the comparator that should be used for comparing the two
     *            columns
     *
     * @return a new greater than or equal node
     */
    public static RuleNode ge(final int leftCol, final int rightCol,
            final DataValueComparator comp) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return comp
                        .compare(row.getCell(leftCol), row.getCell(rightCol)) >= 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + leftCol + "$ " + Operators.GE + " $" + rightCol
                        + "$";
            }
        };
    }

    /**
     * Creates a new greater than or equal node, that compares the value in a
     * column to a fixed number.
     *
     * @param col the left column's index
     * @param value the fixed number on the right side
     *
     * @return a new greater than or equal node
     */
    public static RuleNode ge(final int col, final Number value) {
        if (value instanceof Integer) {
            final int v = value.intValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((IntValue)c).getIntValue() >= v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.GE + " " + value;
                }
            };
        } else {
            final double v = value.doubleValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((DoubleValue)c).getDoubleValue() >= v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.GE + " " + value;
                }
            };
        }
    }

    /**
     * Creates a new greater than or equal node, that compares the value in a
     * column to a fixed string.
     *
     * @param col the left column's index
     * @param value the fixed string on the right side
     *
     * @return a new greater than or equal node
     */
    public static RuleNode ge(final int col, final String value) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c = row.getCell(col);
                if (c.isMissing()) {
                    return false;
                }
                return c.toString().compareTo(value) >= 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col + "$ " + Operators.GE + " \"" + value + "\"";
            }
        };
    }

    /**
     * Creates a new less than node, that compares the values in two columns.
     *
     * @param leftCol the left column's index
     * @param rightCol the right column's index
     * @param comp the comparator that should be used for comparing the two
     *            columns
     *
     * @return a new less than node
     */
    public static RuleNode lt(final int leftCol, final int rightCol,
            final DataValueComparator comp) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return comp
                        .compare(row.getCell(leftCol), row.getCell(rightCol)) < 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + leftCol + "$ " + Operators.LT + " $" + rightCol
                        + "$";
            }
        };
    }

    /**
     * Creates a new less than node, that compares the value in a column to a
     * fixed number.
     *
     * @param col the left column's index
     * @param value the fixed number on the right side
     *
     * @return a new less than node
     */
    public static RuleNode lt(final int col, final Number value) {
        if (value instanceof Integer) {
            final int v = value.intValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((IntValue)c).getIntValue() < v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.LT + " " + value;
                }

            };
        } else {
            final double v = value.doubleValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((DoubleValue)c).getDoubleValue() < v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.LT + " " + value;
                }
            };
        }
    }

    /**
     * Creates a new less than node, that compares the value in a column to a
     * fixed string.
     *
     * @param col the left column's index
     * @param value the fixed string on the right side
     *
     * @return a new less than node
     */
    public static RuleNode lt(final int col, final String value) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c = row.getCell(col);
                if (c.isMissing()) {
                    return false;
                }
                return c.toString().compareTo(value) < 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col + "$ " + Operators.LT + " \"" + value + "\"";
            }
        };
    }

    /**
     * Creates a new less than or equal node, that compares the values in two
     * columns.
     *
     * @param leftCol the left column's index
     * @param rightCol the right column's index
     * @param comp the comparator that should be used for comparing the two
     *            columns
     *
     * @return a new less than or equal node
     */
    public static RuleNode le(final int leftCol, final int rightCol,
            final DataValueComparator comp) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return comp
                        .compare(row.getCell(leftCol), row.getCell(rightCol)) <= 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + leftCol + "$ " + Operators.LE + " $" + rightCol
                        + "$";
            }
        };
    }

    /**
     * Creates a new less than or equal node, that compares the value in a
     * column to a fixed number.
     *
     * @param col the left column's index
     * @param value the fixed number on the right side
     *
     * @return a new greater than node
     */
    public static RuleNode le(final int col, final Number value) {
        if (value instanceof Integer) {
            final int v = value.intValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((IntValue)c).getIntValue() <= v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.LE + " " + value;
                }
            };
        } else {
            final double v = value.doubleValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((DoubleValue)c).getDoubleValue() <= v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.LE + " " + value;
                }
            };
        }
    }

    /**
     * Creates a new less than or equal node, that compares the value in a
     * column to a fixed string.
     *
     * @param col the left column's index
     * @param value the fixed string on the right side
     *
     * @return a new less than or equal node
     */
    public static RuleNode le(final int col, final String value) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c = row.getCell(col);
                if (c.isMissing()) {
                    return false;
                }
                return c.toString().compareTo(value) <= 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col + "$ " + Operators.LE + " \"" + value + "\"";
            }
        };
    }

    /**
     * Creates a equal node, that compares the values in two columns.
     *
     * @param leftCol the left column's index
     * @param rightCol the right column's index
     *
     * @return a new equal node
     */
    public static RuleNode eq(final int leftCol, final int rightCol) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return row.getCell(leftCol).equals(row.getCell(rightCol));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + leftCol + "$ " + Operators.EQ + " $" + rightCol
                        + "$";
            }
        };
    }

    /**
     * Creates a equal node, that compares the value in a column to a fixed
     * number.
     *
     * @param col the left column's index
     * @param value the fixed number on the right side
     *
     * @return a new equal node
     */
    public static RuleNode eq(final int col, final Number value) {
        if (value instanceof Integer) {
            final int v = value.intValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((IntValue)c).getIntValue() == v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.EQ + " " + value;
                }
            };
        } else {
            final double v = value.doubleValue();
            return new RuleNode() {
                public boolean evaluate(final DataRow row) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        return false;
                    }
                    return ((DoubleValue)c).getDoubleValue() == v;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String toString() {
                    return "$" + col + "$ " + Operators.EQ + " " + value;
                }
            };
        }
    }

    /**
     * Creates a new equal node, that compares the value in a column to a fixed
     * string.
     *
     * @param col the left column's index
     * @param value the fixed string on the right side
     *
     * @return a new equal node
     */
    public static RuleNode eq(final int col, final String value) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c = row.getCell(col);
                if (c.isMissing()) {
                    return false;
                }
                return c.toString().equals(value);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col + "$ " + Operators.EQ + " \"" + value + "\"";
            }
        };
    }

    /**
     * Returns a new like not that tries to match a fixed wildcard expression to
     * the value in a column.
     *
     * @param col the left column's index
     * @param value a string that is interpreted as a wildcard pattern (with *
     *            and ? as wildcards)
     *
     * @return a new like node
     */
    public static RuleNode like(final int col, final String value) {
        String regex = WildcardMatcher.wildcardToRegex(value);
        final Pattern p = Pattern.compile(regex);

        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c = row.getCell(col);
                if (c.isMissing()) {
                    return false;
                }
                return p.matcher(c.toString()).matches();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col + "$ " + Operators.LIKE + " \"" + value + "\"";
            }
        };
    }

    /**
     * Returns a new like not that tries to match a wildcard expression in a
     * column to a fixed string value.
     *
     * @param value a fixed value
     * @param col the column's index whose contents are interpreted as wildcard
     *            patterns
     *
     * @return a new like node
     */
    public static RuleNode like(final String value, final int col) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c = row.getCell(col);
                if (c.isMissing()) {
                    return false;
                }
                String regex = WildcardMatcher.wildcardToRegex(c.toString());
                return value.matches(regex);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return " \"" + value + "\" " + Operators.LIKE + "$" + col + "$";
            }
        };
    }

    /**
     * Returns a new like not that tries to match a fixed wildcard expression to
     * the value in a column.
     *
     * @param col1 the left column's index
     * @param col2 the right column's index, whose value is interpreted as a
     *            wildcard pattern (with * and ? as wildcards)
     *
     * @return a new like node
     */
    public static RuleNode like(final int col1, final int col2) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                DataCell c1 = row.getCell(col1);
                if (c1.isMissing()) {
                    return false;
                }

                DataCell c2 = row.getCell(col2);
                if (c2.isMissing()) {
                    return false;
                }

                return c1.toString().matches(
                        WildcardMatcher.wildcardToRegex(c2.toString()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col1 + "$ " + Operators.LIKE + " $" + col2 + "$";
            }
        };
    }

    /**
     * Returns a new in node that checks if the the value in the column is equal
     * to at least one string value from the list.
     *
     * @param col the left column's index
     * @param list a list of strings
     * @return a new in node
     */
    public static RuleNode in(final int col, final List<String> list) {
        final String[] temp = list.toArray(new String[list.size()]);

        return new RuleNode() {

            public boolean evaluate(final DataRow row) {
                for (String s : temp) {
                    DataCell c = row.getCell(col);
                    if (c.isMissing()) {
                        continue;
                    }

                    if (c.toString().equals(s)) {
                        return true;
                    }
                }

                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + col + "$ " + Operators.IN + " "
                        + Arrays.toString(temp);
            }
        };
    }

    /**
     * Creates a new MISSING node.
     *
     * @param col the column int the row to be checked
     *
     * @return a new MISSING node
     */
    public static RuleNode missing(final int col) {
        return new RuleNode() {
            public boolean evaluate(final DataRow row) {
                return row.getCell(col).isMissing();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return Operators.MISSING + " $" + col + "$";
            }
        };
    }
}
