/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
                    return ((IntValue)row.getCell(col)).getIntValue() > v;
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
                    return ((DoubleValue)row.getCell(col)).getDoubleValue() > v;
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
                return row.getCell(col).toString().compareTo(value) > 0;
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
                    return ((IntValue)row.getCell(col)).getIntValue() >= v;
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
                    return ((DoubleValue)row.getCell(col)).getDoubleValue() >= v;
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
                return row.getCell(col).toString().compareTo(value) >= 0;
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
                    return ((IntValue)row.getCell(col)).getIntValue() < v;
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
                    return ((DoubleValue)row.getCell(col)).getDoubleValue() < v;
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
                return row.getCell(col).toString().compareTo(value) < 0;
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
                    return ((IntValue)row.getCell(col)).getIntValue() <= v;
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
                    return ((DoubleValue)row.getCell(col)).getDoubleValue() <= v;
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
                return row.getCell(col).toString().compareTo(value) <= 0;
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
                    return ((IntValue)row.getCell(col)).getIntValue() == v;
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
                    return ((DoubleValue)row.getCell(col)).getDoubleValue() == v;
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
                return row.getCell(col).toString().equals(value);
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
                return p.matcher(row.getCell(col).toString()).matches();
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
                String regex =
                        WildcardMatcher.wildcardToRegex(row.getCell(col)
                                .toString());
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
                    if (row.getCell(col).toString().equals(s)) {
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
}
