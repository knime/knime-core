/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on 2013.07.30. by Gabor Bakos
 */
package org.knime.base.util;

import java.util.Comparator;

import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.StringValue;

/**
 * A {@link DataValueComparator} implementation for {@link StringValue} values and a custom comparator.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public final class StringValueComparator extends DataValueComparator {
    private final Comparator<String> m_comparator;

    /**
     * @param comparator A {@link Comparator} to compare {@link String}s of {@link StringValue}.
     */
    public StringValueComparator(final Comparator<String> comparator) {
        this.m_comparator = comparator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int compareDataValues(final DataValue v1, final DataValue v2) {
        if (v1 instanceof StringValue) {
            StringValue s1 = (StringValue)v1;
            if (v2 instanceof StringValue) {
                StringValue s2 = (StringValue)v2;
                return m_comparator.compare(s1.getStringValue(), s2.getStringValue());
            }
        }
        throw new IllegalStateException("Invalid argument to sort: " + v1 + "\n(" + v1.getClass() + ")\nor " + v2
            + "\n(" + v2.getClass() + ")");
    }
}
