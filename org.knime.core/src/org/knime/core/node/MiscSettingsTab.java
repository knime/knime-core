/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
        return "Memory Policy";
    }
}
