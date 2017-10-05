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
 * Created on Nov 28, 2013 by Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.base.node.meta.looper;

import javax.swing.JCheckBox;

/**
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
public class LoopEnd2NodeDialog extends AbstractLoopEndNodeDialog<LoopEnd2NodeSettings> {

    private final JCheckBox m_ignoreEmptyTables1 = new JCheckBox("Ignore empty input tables at port 1");
    private final JCheckBox m_ignoreEmptyTables2 = new JCheckBox("Ignore empty input tables at port 2");

    private final JCheckBox m_tolerateColumnTypes1 = new JCheckBox("Allow variable column types at port 1");
    private final JCheckBox m_tolerateColumnTypes2 = new JCheckBox("Allow variable column types at port 2");

    private final JCheckBox m_tolerateChangingSpecs1 = new JCheckBox("Allow changing table specifications at port 1");
    private final JCheckBox m_tolerateChangingSpecs2 = new JCheckBox("Allow changing table specifications at port 2");

    /**
     *
     */
    public LoopEnd2NodeDialog() {
        super(new LoopEnd2NodeSettings());
        addComponent(m_ignoreEmptyTables1);
        addComponent(m_ignoreEmptyTables2);
        addComponent(m_tolerateColumnTypes1);
        addComponent(m_tolerateColumnTypes2);
        addComponent(m_tolerateChangingSpecs1);
        addComponent(m_tolerateChangingSpecs2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addToSettings(final LoopEnd2NodeSettings settings) {
        settings.ignoreEmptyTables1(m_ignoreEmptyTables1.isSelected());
        settings.ignoreEmptyTables2(m_ignoreEmptyTables2.isSelected());
        settings.tolerateColumnTypes1(m_tolerateColumnTypes1.isSelected());
        settings.tolerateColumnTypes2(m_tolerateColumnTypes2.isSelected());
        settings.tolerateChangingTableSpecs1(m_tolerateChangingSpecs1.isSelected());
        settings.tolerateChangingTableSpecs2(m_tolerateChangingSpecs2.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadFromSettings(final LoopEnd2NodeSettings settings) {
        m_ignoreEmptyTables1.setSelected(settings.ignoreEmptyTables1());
        m_ignoreEmptyTables2.setSelected(settings.ignoreEmptyTables2());
        m_tolerateColumnTypes1.setSelected(settings.tolerateColumnTypes1());
        m_tolerateColumnTypes2.setSelected(settings.tolerateColumnTypes2());
        m_tolerateChangingSpecs1.setSelected(settings.tolerateChangingTableSpecs1());
        m_tolerateChangingSpecs2.setSelected(settings.tolerateChangingTableSpecs2());
    }

}
