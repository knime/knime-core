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
 *   Dec 13, 2024 (hornm): created
 */
package org.knime.core.node;

/**
 * Marker interface for objects representing aspects of a node that are created via a fluent API. <br>
 * <br>
 *
 * Fluent API design principles:
 * <ul>
 * <li>no build-method</li>
 * <li>the entry for the 'top-level' object API is a static create method</li>
 * <li>required properties first</li>
 * <li>optional properties last</li>
 * <li>no prefixes for setters</li>
 * <li>'add'-prefix to allow multiple invocations (e.g. lists)</li>
 * <li>method/lambda parameters are defined in single objects, i.e. input/output objects; i.e. no combinatoric parameter
 * variations we'd need to account for</li>
 * <li>complex parameters/nested objects follow same fluent API pattern (and implement {@link FluentNodeAPI}); however,
 * no need to call 'create' on nested objects since the 'fluent API instance' is passed in as parameter</li>
 * <li>implementations are final because the fluent API is meant to create the respective objects by composition only
 * and must not be designed in a way to enable inheritance, too
 * <li>implementing classes have only private constructors</li>
 * <li>classes/interfaces representing mandatory properties (i.e. stages) in the fluent API are prefixed with
 * 'Required'</li>
 * </ul>
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public interface FluentNodeAPI {
}
