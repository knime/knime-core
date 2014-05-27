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
 *   25.03.2014 (Christian Albrecht, KNIME.com AG, Zurich, Switzerland): created
 */
package org.knime.core.node.wizard;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @since 2.10
 */
public class WizardNodeLayoutInfo implements Cloneable {

    private String m_x;
    private String m_y;
    private String m_padding;

    /**
     * Constructs a new (invalid) layout info object.
     */
    public WizardNodeLayoutInfo() {

    }

    /**
     * Constructs a new (valid) layout info object
     * @param x x coordinates
     * @param y y coordinates
     *
     */
    public WizardNodeLayoutInfo(final String x, final String y) {
        setX(x);
        setY(y);
    }

    /**
     * @return the x
     */
    public String getX() {
        return m_x;
    }

    /**
     * @param x the x to set
     */
    public void setX(final String x) {
        m_x = x;
    }

    /**
     * @return the y
     */
    public String getY() {
        return m_y;
    }

    /**
     * @param y the y to set
     */
    public void setY(final String y) {
        m_y = y;
    }

    /**
     * @return the padding
     */
    public String getPadding() {
        return m_padding;
    }

    /**
     * @param padding the padding to set
     */
    public void setPadding(final String padding) {
        m_padding = padding;
    }

    /**
     * Saves a layout info object to node settings.
     * @param settings the node settings to save to
     * @param layoutInfo the layout info object to save, can be null
     */
    public static void saveToNodeSettings(final NodeSettingsWO settings, final WizardNodeLayoutInfo layoutInfo) {
        if (layoutInfo != null) {
            settings.addBoolean("hasLayout", true);
            settings.addString("layout_x", layoutInfo.getX());
            settings.addString("layout_y", layoutInfo.getY());
            settings.addString("layout_padding", layoutInfo.getPadding());
        } else {
            settings.addBoolean("hasLayout", false);
        }
    }

    /**
     * Loads a layout info object from node settings.
     * @param settings the node settings to load from
     * @return the layout info object or null, if none exists
     */
    public static WizardNodeLayoutInfo loadFromNodeSettings(final NodeSettingsRO settings) {
        if (settings.getBoolean("hasLayout", false)) {
            try {
                String x = settings.getString("layout_x");
                String y = settings.getString("layout_y");
                String padding = settings.getString("layout_padding", null);
                WizardNodeLayoutInfo layoutInfo = new WizardNodeLayoutInfo(x, y);
                layoutInfo.setPadding(padding);
                return layoutInfo;
            } catch (InvalidSettingsException e) {
                // do nothing
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_padding == null) ? 0 : m_padding.hashCode());
        result = prime * result + ((m_x == null) ? 0 : m_x.hashCode());
        result = prime * result + ((m_y == null) ? 0 : m_y.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WizardNodeLayoutInfo other = (WizardNodeLayoutInfo)obj;
        if (m_padding == null) {
            if (other.m_padding != null) {
                return false;
            }
        } else if (!m_padding.equals(other.m_padding)) {
            return false;
        }
        if (m_x == null) {
            if (other.m_x != null) {
                return false;
            }
        } else if (!m_x.equals(other.m_x)) {
            return false;
        }
        if (m_y == null) {
            if (other.m_y != null) {
                return false;
            }
        } else if (!m_y.equals(other.m_y)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "WizardNodeLayoutInfo [x=" + m_x + ", y=" + m_y + ", padding=" + m_padding + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WizardNodeLayoutInfo clone() {
        try {
            return (WizardNodeLayoutInfo)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

}
