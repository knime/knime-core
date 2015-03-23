/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   13.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel.droppanes;

import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.preproc.draganddroppanel.PaneConfigurationDialog;

/**
 *
 * @author tibuch
 */
public class DropPaneConfig implements Comparable<DropPaneConfig> {

    private int m_position = -1;

    private ArrayList<String> m_columnNames = new ArrayList<String>();

    private PaneConfigurationDialog m_dialog = null;

    /**
     * @return
     */
    public String getSelectionAsString() {
        String r = "";
        for (int i = 0; i < m_columnNames.size(); i++) {
            r += m_columnNames.get(i).trim();
            if (i != m_columnNames.size() - 1) {
                r += "\n";
            }
        }
        return r;
    }

    /**
     * @return the position
     */
    public int getPosition() {
        return m_position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(final int position) {
        m_position = position;
    }

    /**
     * @return the columnNames
     */
    public List<String> getSelection() {
        return m_columnNames;
    }

    /**
     * @return the dialog
     */
    public PaneConfigurationDialog getDialog() {
        return m_dialog;
    }

    /**
     * @param dialog the dialog to set
     */
    public void setDialog(final PaneConfigurationDialog dialog) {
        m_dialog = dialog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final DropPaneConfig o) {
        if (m_position < o.getPosition()) {
            return -1;
        } else if (m_position == o.getPosition()) {
            return 0;
        } else {
            return 1;
        }
    }
}
