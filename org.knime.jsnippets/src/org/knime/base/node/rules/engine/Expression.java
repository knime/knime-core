/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on 2013.05.03. by Gabor
 */
package org.knime.base.node.rules.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;

/**
 * A typed expression that can be evaluated using a {@link DataRow} and a {@link VariableProvider}.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public interface Expression {
    /**
     * The type of the expression.
     *
     * @since 2.9
     */
    public enum ASTType {
        /** Reference to a column. */
        ColRef,
        /** Reference to a flow variable */
        FlowVarRef,
        /** Reference to a table property. */
        TableRef,
        /** A constant. */
        Constant,
        /** {@code <} */
        Less,
        /** {@code =} */
        Equals,
        /** {@code >} */
        Greater,
        /** {@code <=} */
        LessOrEquals,
        /** {@code >=} */
        GreaterOrEquals,
        /** Missing operator. */
        Missing,
        /** List constructor. */
        List,
        /** In operator. */
        In,
        /** Regular expression match. */
        Matches,
        /** SQL-like like. @see org.knime.base.util.WildcardMatcher */
        Like,
        /** Logical negation. */
        Not,
        /** Logical conjunction. */
        And,
        /** Logical disjunction. */
        Or,
        /** Xor. */
        Xor;
    }

    /**
     * @return {@link DataType} of input arguments (can be empty).
     */
    List<DataType> getInputArgs();

    /**
     * @return {@link DataType} of output.
     */
    DataType getOutputType();

    /**
     * Computes the value of the {@link Expression}.
     *
     * @param row A {@link DataRow}.
     * @param provider The {@link VariableProvider}.
     * @return The result of the evaluation.
     */
    ExpressionValue evaluate(DataRow row, VariableProvider provider);

    /**
     * @return {@code true} means it can be evaluated during construction.
     */
    boolean isConstant();

    /** @return The type of the {@link Expression}. */
    ASTType getTreeType();

    /** @return The contained sub{@link Expression}s. */
    List<Expression> getChildren();

    /**
     * Base class for {@link Expression}s. It handles {@link #getChildren()}. No setter for them.
     *
     * @since 2.9
     */
    public abstract class Base implements Expression {
        private final List<Expression> m_children;

        /**
         * Constructs {@link Expression} using its children.
         * @param children The contained subexpressions.
         */
        public Base(final Expression... children) {
            this.m_children = Arrays.asList(children.clone());
        }

        /**
         * Constructs {@link Expression} using its children.
         * @param children The contained subexpressions.
         */
        public Base(final List<Expression> children) {
            this.m_children = new ArrayList<Expression>(children);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Expression> getChildren() {
            return Collections.unmodifiableList(m_children);
        }
    }
}
