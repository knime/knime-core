/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Jun 3, 2010 (wiswedel): created
 */
package org.knime.base.node.meta.looper.chunk;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.meta.looper.chunk.LoopStartChunkConfiguration.Mode;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class LoopStartChunkNodeDialogPane extends NodeDialogPane {

    private final JRadioButton m_rowsPerChunkButton;
    private final JRadioButton m_chunkCountButton;
    private final JSpinner m_rowsPerChunkSpinner;
    private final JSpinner m_chunkCountSpinner;

    /**
     *
     */
    public LoopStartChunkNodeDialogPane() {
        ButtonGroup bg = new ButtonGroup();
        m_rowsPerChunkButton = new JRadioButton("Rows per chunk");
        m_chunkCountButton = new JRadioButton("No. of chunks");
        ActionListener al = new ActionListener() {
            /** {@inheritDoc} */
            public void actionPerformed(final ActionEvent e) {
                onNewSelection();
            }
        };
        m_rowsPerChunkButton.addActionListener(al);
        m_chunkCountButton.addActionListener(al);
        bg.add(m_rowsPerChunkButton);
        bg.add(m_chunkCountButton);
        m_chunkCountSpinner = new JSpinner(new SpinnerNumberModel(
                10, 1, Integer.MAX_VALUE, 5));
        m_rowsPerChunkSpinner = new JSpinner(new SpinnerNumberModel(
                10, 1, Integer.MAX_VALUE, 10));
        m_rowsPerChunkButton.doClick();
        initLayout();
    }

    /**
     *
     */
    private void initLayout() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(getInFlowLayout(m_rowsPerChunkButton));
        panel.add(getInFlowLayout(m_rowsPerChunkSpinner));
        panel.add(getInFlowLayout(m_chunkCountButton));
        panel.add(getInFlowLayout(m_chunkCountSpinner));
        addTab("Configuration", panel);
    }

    /**
     * @param rowsPerChunkButton
     * @return
     */
    private JPanel getInFlowLayout(final JComponent... comps) {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            result.add(c);
        }
        return result;
    }

    /**
     *
     */
    private void onNewSelection() {
        boolean isRowCountPerChunk = m_rowsPerChunkButton.isSelected();
        m_chunkCountSpinner.setEnabled(!isRowCountPerChunk);
        m_rowsPerChunkSpinner.setEnabled(isRowCountPerChunk);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        LoopStartChunkConfiguration config = new LoopStartChunkConfiguration();
        config.loadSettingsInDialog(settings);
        m_chunkCountSpinner.setValue(config.getNrOfChunks());
        m_rowsPerChunkSpinner.setValue(config.getNrRowsPerChunk());
        switch (config.getMode()) {
        case RowsPerChunk:
            m_rowsPerChunkButton.doClick();
            break;
        default:
            m_chunkCountButton.doClick();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        LoopStartChunkConfiguration config = new LoopStartChunkConfiguration();
        config.setNrOfChunks((Integer)m_chunkCountSpinner.getValue());
        config.setNrRowsPerChunk((Integer)m_rowsPerChunkSpinner.getValue());
        if (m_rowsPerChunkButton.isSelected()) {
            config.setMode(Mode.RowsPerChunk);
        } else {
            config.setMode(Mode.NrOfChunks);
        }
        config.saveSettingsTo(settings);
    }

}
