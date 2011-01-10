/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 8, 2010 (wiswedel): created
 */
package org.knime.base.node.io.extractsysprop;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ReadSysPropertyNodeDialogPane extends NodeDialogPane {

    private final ColumnFilterPanel m_filterPanel;
    private final JCheckBox m_failIfNotPresentChecker;
    private final JCheckBox m_extractAllPropertiesChecker;

    /**
     *
     */
    ReadSysPropertyNodeDialogPane() {
        m_filterPanel = new ColumnFilterPanel(false);
        m_failIfNotPresentChecker = new JCheckBox(
                "Fail if property not present in runtime environment");
        m_extractAllPropertiesChecker = new JCheckBox(
                "Extract all available properties");
        m_extractAllPropertiesChecker.addItemListener(new ItemListener() {
            /** {@inheritDoc} */
            public void itemStateChanged(final ItemEvent e) {
                onExtractAllChange();
            }

        });
        JPanel panel = new JPanel(new BorderLayout());
        JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER));
        north.add(m_failIfNotPresentChecker);
        north.add(m_extractAllPropertiesChecker);
        panel.add(north, BorderLayout.NORTH);
        panel.add(m_filterPanel, BorderLayout.CENTER);
        m_extractAllPropertiesChecker.doClick();
        addTab("Properties", panel);
    }

    private void onExtractAllChange() {
        boolean sel = m_extractAllPropertiesChecker.isSelected();
        m_failIfNotPresentChecker.setEnabled(!sel);
        m_filterPanel.setEnabled(!sel);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ReadSysPropertyConfiguration c = new ReadSysPropertyConfiguration();
        c.setExtractAllProps(m_extractAllPropertiesChecker.isSelected());
        c.setFailIfSomeMissing(m_failIfNotPresentChecker.isSelected());
        Set<String> ins = m_filterPanel.getIncludedColumnSet();
        String[] included = ins.toArray(new String[ins.size()]);
        c.setSelectedProps(included);
        c.saveSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        ReadSysPropertyConfiguration c = new ReadSysPropertyConfiguration();
        c.loadSettingsNoFail(settings);
        Map<String, String> allProps =
            ReadSysPropertyConfiguration.readAllProps();
        DataColumnSpec[] propsAsCols = new DataColumnSpec[allProps.size()];
        int index = 0;
        for (String s : allProps.keySet()) {
            propsAsCols[index++] =
                new DataColumnSpecCreator(s, StringCell.TYPE).createSpec();
        }
        DataTableSpec allPropsAsTable = new DataTableSpec(propsAsCols);
        m_extractAllPropertiesChecker.setSelected(c.isExtractAllProps());
        if (c.isExtractAllProps()) {
            m_filterPanel.update(allPropsAsTable, true, Collections.EMPTY_SET);
            m_failIfNotPresentChecker.setSelected(false);
        } else {
            m_filterPanel.update(allPropsAsTable, false, c.getSelectedProps());
            m_failIfNotPresentChecker.setSelected(c.isFailIfSomeMissing());
        }
    }

}
