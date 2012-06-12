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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   20.10.2008 (ohl): created
 */
package org.knime.core.node;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.data.container.DataContainer;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;

/**
 * Implements the tab for the memory policy of nodes with output ports. This tab
 * will be inserted by the framework into the node's dialog, if the node has
 * out-ports.
 *
 * @author ohl, University of Konstanz
 */
class MiscSettingsTab extends JPanel {
    public static final String MEMORY_POLICY = "Memory Policy";
	private final ButtonGroup m_group;

    /** Inits GUI. */
    public MiscSettingsTab() {
        super(new BorderLayout());
        m_group = new ButtonGroup();
        JRadioButton cacheAll = new JRadioButton("Keep all in memory.");
        cacheAll.setActionCommand(MemoryPolicy.CacheInMemory.toString());
        m_group.add(cacheAll);
        cacheAll.setToolTipText(
                "All generated output data is kept in main memory, "
                + "resulting in faster execution of successor nodes but "
                + "also in more memory usage.");
        JRadioButton cacheSmall = new JRadioButton(
                "Keep only small tables in memory.", true);
        cacheSmall.setActionCommand(MemoryPolicy.CacheSmallInMemory.toString());
        m_group.add(cacheSmall);
        cacheSmall.setToolTipText("Tables with less than "
                + DataContainer.MAX_CELLS_IN_MEMORY + " cells are kept in "
                + "main memory, otherwise swapped to disc.");
        JRadioButton cacheOnDisc = new JRadioButton(
                "Write tables to disc.");
        cacheOnDisc.setActionCommand(MemoryPolicy.CacheOnDisc.toString());
        m_group.add(cacheOnDisc);
        cacheOnDisc.setToolTipText("All output is immediately "
                + "written to disc to save main memory usage.");
        final int s = 15;
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, s, s));
        north.add(new JLabel("Select memory policy for data outport(s)"));
        add(north, BorderLayout.NORTH);
        JPanel bigCenter =
            new JPanel(new FlowLayout(FlowLayout.LEFT, s, s));
        JPanel center = new JPanel(new GridLayout(0, 1));
        center.add(cacheAll);
        center.add(cacheSmall);
        center.add(cacheOnDisc);
        bigCenter.add(center);
        add(bigCenter, BorderLayout.CENTER);
    }

    /** Get the memory policy for the currently selected radio button.
     * @return The corresponding policy.
     */
    MemoryPolicy getStatus() {
        String memoryPolicy = m_group.getSelection().getActionCommand();
        return MemoryPolicy.valueOf(memoryPolicy);
    }

    /** Select the radio button for the given policy.
     * @param policy The one to use.
     */
    void setStatus(final MemoryPolicy policy) {
        for (Enumeration<AbstractButton> e = m_group.getElements();
            e.hasMoreElements();) {
            AbstractButton m = e.nextElement();
            if (m.getActionCommand().equals(policy.toString())) {
                m.setSelected(true);
                return;
            }
        }
        assert false;
    }

    String getTabName() {
        return MEMORY_POLICY;
    }
}
