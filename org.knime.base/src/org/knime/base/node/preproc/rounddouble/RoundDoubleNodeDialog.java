/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *   03.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.rounddouble;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Creates the dialog of the round double node and provides static methods
 * which create the necessary settings models.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class RoundDoubleNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Creates and returns the settings model, storing the selected columns.
     *
     * @return The settings model with the selected columns.
     */
    @SuppressWarnings("unchecked")
    static final SettingsModelColumnFilter2 getFilterDoubleColModel() {
        return new SettingsModelColumnFilter2(RoundDoubleConfigKeys.COLUMN_NAMES, new Class[]{DoubleValue.class});
    }

    /**
     * Creates and returns the settings model, storing the precision number.
     *
     * @return The settings model with the precision number.
     */
    static final SettingsModelIntegerBounded getNumberPrecisionModel() {
        return new SettingsModelIntegerBounded(
                RoundDoubleConfigKeys.PRECISION_NUMBER,
                RoundDoubleNodeModel.DEF_PRECISION,
                RoundDoubleNodeModel.MIN_PRECISION,
                RoundDoubleNodeModel.MAX_PRECISION);
    }

    /**
     * Creates and returns the settings model, storing the "append column" flag.
     *
     * @return The settings model with the "append column" flag.
     */
    static final SettingsModelBoolean getAppendColumnModel() {
        return new SettingsModelBoolean(RoundDoubleConfigKeys.APPEND_COLUMNS,
                RoundDoubleNodeModel.DEF_APPEND_COLUMNS);
    }

    /**
     * Creates and returns the settings model, storing the column suffix.
     *
     * @return The settings model with the column suffix.
     */
    static final SettingsModelString getColumnSuffixModel() {
        return new SettingsModelString(RoundDoubleConfigKeys.COLUMN_SUFFIX,
                RoundDoubleNodeModel.DEF_COLUMN_SUFFIX);
    }

    /**
     * Creates and returns the settings model, storing the rounding mode.
     *
     * @return The settings model with the rounding mode.
     */
    static final SettingsModelString getRoundingModelStringModel() {
        return new SettingsModelString(RoundDoubleConfigKeys.ROUNDING_MODE,
                RoundDoubleNodeModel.DEF_ROUNDING_MODE);
    }

    /**
     * Creates and returns the settings model, storing the "output as string"
     * flag.
     *
     * @return The settings model with the "output as string" flag.
     */
    static final SettingsModelBoolean getOutputAsStringModel() {
        return new SettingsModelBoolean(RoundDoubleConfigKeys.OUTPUT_AS_STRING,
                RoundDoubleNodeModel.DEF_OUTPUT_AS_STRING);
    }

    private SettingsModelString m_suffixModel;
    private SettingsModelBoolean m_appendColumnModel;

    /**
     * Creates new instance of <code>RoundDoubleNodeDialog</code>.
     */
    public RoundDoubleNodeDialog() {
        // COLUMN SELECTION
        createNewGroup("Column selection");
        addDialogComponent(new DialogComponentColumnFilter2(getFilterDoubleColModel(), 0));
        closeCurrentGroup();

        // COLUMN SETTINGS
        createNewGroup("Column settings");
        setHorizontalPlacement(true);
        m_appendColumnModel = getAppendColumnModel();
        m_suffixModel = getColumnSuffixModel();

        m_appendColumnModel.addChangeListener(new AppendColumnChanceListener());
        addDialogComponent(new DialogComponentBoolean(m_appendColumnModel,
        "Append rounded values as additional columns"));

        addDialogComponent(new DialogComponentString(m_suffixModel,
                "Column suffix"));

        addDialogComponent(new DialogComponentBoolean(getOutputAsStringModel(),
                "Format as string"));

        setHorizontalPlacement(false);
        closeCurrentGroup();

        // ROUNDING SETTINGS
        createNewGroup("Rounding settings");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentNumber(getNumberPrecisionModel(),
                "Decimal place", 1));

        addDialogComponent(new DialogComponentStringSelection(
                getRoundingModelStringModel(), "Rounding mode",
                RoundDoubleNodeModel.ROUNDING_MODES));
        setHorizontalPlacement(false);
        closeCurrentGroup();
    }

    private class AppendColumnChanceListener implements ChangeListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void stateChanged(final ChangeEvent e) {
            m_suffixModel.setEnabled(m_appendColumnModel.getBooleanValue());
        }
    }
}
