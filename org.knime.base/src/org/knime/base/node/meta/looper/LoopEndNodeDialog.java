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
 * Created on Nov 28, 2013 by Patrick Winter, KNIME AG, Zurich, Switzerland
 */
package org.knime.base.node.meta.looper;

import javax.swing.JCheckBox;

/**
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class LoopEndNodeDialog extends AbstractLoopEndNodeDialog<LoopEndNodeSettings> {

    private final JCheckBox m_ignoreEmptyTables = new JCheckBox("Ignore empty input tables");

    private final JCheckBox m_tolerateColumnTypes = new JCheckBox("Allow variable column types");

    private final JCheckBox m_tolerateChangingSpecs = new JCheckBox("Allow changing table specifications");

    /** Create a new dialog.  */
    public LoopEndNodeDialog() {
        super(new LoopEndNodeSettings());
        addComponent(m_ignoreEmptyTables);
        addComponent(m_tolerateColumnTypes);
        addComponent(m_tolerateChangingSpecs);
    }

    /**
     * {@inheritDoc}
     * @since 2.9
     */
    @Override
    protected void addToSettings(final LoopEndNodeSettings settings) {
        settings.ignoreEmptyTables(m_ignoreEmptyTables.isSelected());
        settings.tolerateColumnTypes(m_tolerateColumnTypes.isSelected());
        settings.tolerateChangingTableSpecs(m_tolerateChangingSpecs.isSelected());
    }

    /**
     * {@inheritDoc}
     * @since 2.9
     */
    @Override
    protected void loadFromSettings(final LoopEndNodeSettings settings) {
        m_ignoreEmptyTables.setSelected(settings.ignoreEmptyTables());
        m_tolerateColumnTypes.setSelected(settings.tolerateColumnTypes());
        m_tolerateChangingSpecs.setSelected(settings.tolerateChangingTableSpecs());
    }


}
