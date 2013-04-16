/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * Created on 21.03.2013 by peter
 */
package org.knime.core.node.defaultnodesettings;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * SettingsModel for the DialogComponentDate-Dialog storing an user specified date and the additional information to
 * reconstruct the Dialog during load.
 *
 *
 * @author Sebastian Peter, University of Konstanz
 * @since 2.8
 */
public class SettingsModelDate extends SettingsModel {

    private static final String VALUEKEY = "VALUEKEYSETTINGS";

    private static final String STATUSKEY = "STATUSKEYSETTINGS";

    private long m_millies = 0;

    private int m_status = 0;

    private final String m_configName;

    /**
     * Returns the saved Date in milliseconds since 1970 in UTC time.
     *
     * @return time
     */
    public long getTimeInMillis() {
        return m_millies;
    }

    /**
     * Returns the stored date.
     *
     * @return the user specified date configured in UTC
     */
    public Date getDate() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(m_millies);

        return cal.getTime();

    }

    /**
     * sets the status code for the selected fields of the DateInputDialog.
     *
     * @param fields status code for the fields of the corresponding DateInputDialog
     *
     */
    public void setSelectedFields(final int fields) {
        m_status = fields;
    }

    /**
     * sets the Date specified in the dialog in the long format.
     *
     * @param time time in milliseconds corresponding to the selected Date
     */
    public void setTimeInMillis(final long time) {
        m_millies = time;
    }

    /**
     * sets the status code for the selected fields of the DateInputDialog.
     *
     * @return fields status code for the fields of the corresponding DateInputDialog
     */
    public int getSelectedFields() {
        return m_status;
    }

    /**
     * Creates a new object holding a date value.
     *
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     */
    public SettingsModelDate(final String configName) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }
        m_configName = configName;
    }

    private SettingsModelDate(final String configname, final long value, final int status) {
        m_millies = value;
        m_status = status;
        m_configName = configname;

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDate createClone() {
        return new SettingsModelDate(m_configName, m_millies, m_status);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_DATE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            m_millies = settings.getLong(m_configName + VALUEKEY);
            m_status = settings.getInt(m_configName + STATUSKEY);
        } catch (InvalidSettingsException e) {
            m_millies = 0;
            m_status = 0;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addLong(m_configName + VALUEKEY, m_millies);
        settings.addInt(m_configName + STATUSKEY, m_status);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_millies = settings.getLong(m_configName + VALUEKEY);
        m_status = settings.getInt(m_configName + STATUSKEY);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addLong(m_configName + VALUEKEY, m_millies);
        settings.addInt(m_configName + STATUSKEY, m_status);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }
}
