/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   31.10.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
 * @author ohl, University of Konstanz
 */
public class SettingsModelFilterString extends SettingsModel {

    private static final String CFGKEY_INCL = "InclList";

    private static final String CFGKEY_EXCL = "ExclList";

    private final String m_configName;

    private final List<String> m_inclList;

    private final List<String> m_exclList;

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
                            "The include and exclude"
                                    + "lists contain the same object.");
                }
                if (!m_exclList.contains(e)) {
                    m_exclList.add(e);
                }
            }
        }

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
        this(configName, Arrays.asList(defaultInclList), Arrays
                .asList(defaultExclList));
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
                        m_exclList));
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
            // the way we do this, partially correct settings will be parially
            // transferred into the dialog. Which is okay, I guess.
            setIncludeList(lists.getStringArray(CFGKEY_INCL, (String[])null));
            setExcludeList(lists.getStringArray(CFGKEY_EXCL, (String[])null));
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
        // figure out if we need to notify listeners
        boolean notify = (newValue.size() != m_inclList.size());
        if (!notify) {
            // if the size is the same we need to compare each list item (the
            // order of the lists doesn't matter)!
            for (String s : m_inclList) {
                if (!newValue.contains(s)) {
                    notify = true;
                    break;
                }
            }
        }

        // now take over the new list
        m_inclList.clear();
        m_inclList.addAll(newValue);
                
        // if we got a different list we need to let all listeners know.
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
     * set the value of the stored exclude list.
     * 
     * @param newValue the new value to store as exclude list.
     */
    public void setExcludeList(final String[] newValue) {
        setExcludeList(Arrays.asList(newValue));
    }

    /**
     * set the value of the stored exclude list.
     * 
     * @param newValue the new value to store as exclude list. Can't be null.
     */
    public void setExcludeList(final Collection<String> newValue) {
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
                
        // if we got a different list we need to let all listeners know.
        if (notify) {
            notifyChangeListeners();
        }
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
