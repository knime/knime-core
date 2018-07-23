/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   04.06.2018 (Jonathan Hale): created
 */
package org.knime.core.data.convert.map;

import org.knime.core.data.convert.map.Source.ProducerParameters;

/**
 * Simple implementation of {@link CellValueProducer} that allows passing the production function as a lambda
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <S> Type of source this producer reads from
 * @param <ET> Type of the external type
 * @param <T> Java type that is produced
 * @param <PP> Producer parameter subclass for the source type
 *
 * @since 3.6
 */
public class SimpleCellValueProducerFactory<S extends Source<ET>, ET, T, PP extends ProducerParameters<S>>
    extends AbstractCellValueProducerFactory<S, ET, T, PP> {

    final ET m_externalType;

    final Class<?> m_destType;

    final CellValueProducer<S, T, PP> m_producer;

    /**
     * Constructor
     *
     * @param externalType Identifier of the external type
     * @param destType Target Java type
     * @param producer Cell value producer function (e.g. as lambda)
     */
    public SimpleCellValueProducerFactory(final ET externalType, final Class<?> destType,
        final CellValueProducer<S, T, PP> producer) {
        m_externalType = externalType;
        m_destType = destType;
        m_producer = producer;
    }

    @Override
    public String getIdentifier() {
        return m_externalType + "->" + m_destType.getName();
    }

    @Override
    public Class<?> getDestinationType() {
        return m_destType;
    }

    @Override
    public ET getSourceType() {
        return m_externalType;
    }

    @Override
    public CellValueProducer<S, T, PP> create() {
        return m_producer;
    }
}