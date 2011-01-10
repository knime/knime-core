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
 * --------------------------------------------------------------------- *
 *
 * History
 *   31.10.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Implements a settings model that provides include and exclude lists. These
 * lists contain strings. It's currently used e.g. in the column filter
 * component and provides the list of column names to include and exclude.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class SettingsModelFilterString extends SettingsModel {

    private static final String CFGKEY_INCL = "InclList";

    private static final String CFGKEY_EXCL = "ExclList";

    private static final String CFGKEY_KEEPALL = "keep_all_columns_selected";

    private final String m_configName;

    private final List<String> m_inclList;

    private final List<String> m_exclList;

    private boolean m_keepAll = false;

    /**
     * Creates a new object holding a list of strings in an exclude list and a
     * list of strings in an include list..
     *
     * @param configName the identifier the values are stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultInclList the initial value for the include list
     * @param defaultExclList the initial value for the exclude list.
     */
    public SettingsModelFilterString(final String configName,
            final Collection<String> defaultInclList,
            final Collection<String> defaultExclList) {
        this(configName, defaultInclList, defaultExclList, false);
    }

    /**
     * Creates a new object holding a list of strings in an exclude list and a
     * list of strings in an include list..
     *
     * @param configName the identifier the values are stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultInclList the initial value for the include list
     * @param defaultExclList the initial value for the exclude list.
     * @param keepAll true, if all column should be kept selected
     */
    public SettingsModelFilterString(final String configName,
            final Collection<String> defaultInclList,
            final Collection<String> defaultExclList,
            final boolean keepAll) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }

        m_configName = configName;

        m_inclList = new LinkedList<String>();
        m_exclList = new LinkedList<String>();

        if (defaultInclList != null) {
            for (String i : defaultInclList) {
                if (!m_inclList.contains(i)) {
                    // ignore double entries
                    m_inclList.add(i);
                }
            }
        }
        if (defaultExclList != null) {
            for (String e : defaultExclList) {
                // entries can't be in the include and exclude list!
                if (m_inclList.contains(e)) {
                    throw new IllegalArgumentException(
                            "The include and exclude "
                                    + "lists contain the same object.");
                }
                if (!m_exclList.contains(e)) {
                    m_exclList.add(e);
                }
            }
        }
        m_keepAll = keepAll;
    }

    /**
     * Creates a new object holding a list of strings in an exclude list and a
     * list of strings in an include list..
     *
     * @param configName the identifier the values are stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultInclList the initial value for the include list
     * @param defaultExclList the initial value for the exclude list.
     */
    public SettingsModelFilterString(final String configName,
            final String[] defaultInclList, final String[] defaultExclList) {
        this(configName, Arrays.asList(defaultInclList),
                Arrays.asList(defaultExclList), false);
    }

    /**
     * Creates a new object holding a list of strings in an exclude list and a
     * list of strings in an include list.
     *
     * @param configName the identifier the values are stored within the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultInclList the initial value for the include list
     * @param defaultExclList the initial value for the exclude list.
     * @param keepAll true, if all column should be kept selected
     */
    public SettingsModelFilterString(final String configName,
            final String[] defaultInclList, final String[] defaultExclList,
            final boolean keepAll) {
        this(configName, Arrays.asList(defaultInclList),
                Arrays.asList(defaultExclList), keepAll);
    }

    /**
     * Returns the status of the keep-all columns selection box.
     * @return true, if all column should be kept selected
     */
    public boolean isKeepAllSelected() {
        return m_keepAll;
    }

    /**
     * Set a new keep all selection state.
     * @param selected if keep all box is selected or not
     */
    public void setKeepAllSelected(final boolean selected) {
        boolean notify = setKeepAllSelectedNO(selected);
        if (notify) {
            notifyChangeListeners();
        }
    }

    private boolean setKeepAllSelectedNO(final boolean selected) {
        boolean notify = (m_keepAll != selected);
        m_keepAll = selected;
        return notify;
    }

    /**
     * Constructs a new model with initially empty include and exclude lists.
     *
     * @param configName the identifier the values are stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     */
    public SettingsModelFilterString(final String configName) {
        this(configName, (Collection<String>)null, (Collection<String>)null);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelFilterString createClone() {
        return new SettingsModelFilterString(m_configName,
                new LinkedList<String>(m_inclList), new LinkedList<String>(
                        m_exclList), m_keepAll);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_filterString";
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
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            Config lists = settings.getConfig(m_configName);
            // the way we do this, partially correct settings will be partially
            // transferred into the dialog. Which is okay, I guess.
            setIncludeList(lists.getStringArray(CFGKEY_INCL, (String[])null));
            setExcludeList(lists.getStringArray(CFGKEY_EXCL, (String[])null));
            setKeepAllSelected(lists.getBoolean(CFGKEY_KEEPALL, false));
        } catch (IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        } catch (InvalidSettingsException ise) {
            // no settings - keep the old value.
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * set the value of the stored include list.
     *
     * @param newValue the new value to store as include list.
     */
    public void setIncludeList(final String[] newValue) {
        setIncludeList(Arrays.asList(newValue));
    }

    /**
     * set the value of the stored include list.
     *
     * @param newValue the new value to store as include list. Can't be null.
     */
    public void setIncludeList(final Collection<String> newValue) {
        boolean notify = setIncludeListNO(newValue);
        // if we got a different list we need to let all listeners know.
        if (notify) {
            notifyChangeListeners();
        }
    }

    private boolean setIncludeListNO(final Collection<String> newValue) {
        // figure out if we need to notify listeners
        boolean notify = (newValue.size() != m_inclList.size());
        if (!notify) {
            // if the size is the same we need to compare each list item (the
            // order of the lists doesn't matter)!
            Set<String> newSet = new HashSet<String>(newValue);
            for (String s : m_inclList) {
                if (!newSet.contains(s)) {
                    notify = true;
                    break;
                }
            }
        }

        // now take over the new list
        m_inclList.clear();
        m_inclList.addAll(newValue);

        return notify;
    }

    /**
     * Apply new list of inclusion and exclusion columns on the settings model
     * and notifies all registered listeners (e.g. the model).
     * @param incl list of inclusion columns
     * @param excl list of exclusion columns
     * @param keepAll <code>true</code> if the 'Keep all' box is available;
     *        otherwise <code>false</code>
     */
    public final void setNewValues(final Collection<String> incl,
            final Collection<String> excl, final boolean keepAll) {
        boolean notify = setKeepAllSelectedNO(keepAll);
        notify |= setIncludeListNO(incl);
        notify |= setExcludeListNO(excl);
        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * @return the currently stored include list. Don't modify the list.
     */
    public List<String> getIncludeList() {
        return Collections.unmodifiableList(m_inclList);
    }

    /**
     * Set the value of the stored exclude list.
     * @param newValue the new value to store as exclude list.
     */
    public void setExcludeList(final String[] newValue) {
        setExcludeList(Arrays.asList(newValue));
    }

    /**
     * Set the value of the stored exclude list.
     * @param newValue the new value to store as exclude list. Can't be null.
     */
    public void setExcludeList(final Collection<String> newValue) {
        boolean notify = setExcludeListNO(newValue);
        // if we got a different list we need to let all listeners know.
        if (notify) {
            notifyChangeListeners();
        }
    }

    private boolean setExcludeListNO(final Collection<String> newValue) {
        // figure out if we need to notify listeners
        boolean notify = (newValue.size() != m_exclList.size());
        if (!notify) {
            // if the size is the same we need to compare each list item (the
            // order of the lists doesn't matter)!
            for (String s : m_exclList) {
                if (!newValue.contains(s)) {
                    notify = true;
                    break;
                }
            }
        }

        // now take over the new list
        m_exclList.clear();
        m_exclList.addAll(newValue);
        return notify;
    }

    /**
     * @return the currently stored exclude list. Don't modify the list.
     */
    public List<String> getExcludeList() {
        return Collections.unmodifiableList(m_exclList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            Config lists = settings.getConfig(m_configName);
            String[] incl = lists.getStringArray(CFGKEY_INCL);
            String[] excl = lists.getStringArray(CFGKEY_EXCL);
            setIncludeList(incl);
            setExcludeList(excl);
            boolean keepAll = lists.getBoolean(CFGKEY_KEEPALL, false);
            setKeepAllSelected(keepAll);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        Config lists = settings.addConfig(m_configName);
        lists.addStringArray(CFGKEY_INCL, getIncludeList().toArray(
                new String[0]));
        lists.addStringArray(CFGKEY_EXCL, getExcludeList().toArray(
                new String[0]));
        lists.addBoolean(CFGKEY_KEEPALL, m_keepAll);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // expect a sub-config with two string arrays: include and exclude list
        Config lists = settings.getConfig(m_configName);
        lists.getStringArray(CFGKEY_INCL);
        lists.getStringArray(CFGKEY_EXCL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

}
