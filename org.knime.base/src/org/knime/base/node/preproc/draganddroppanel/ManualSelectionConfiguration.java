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
 *   12.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.preproc.draganddroppanel.droppanes.DropPaneConfig;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.util.Pair;

/**
 *
 * @author tibuch
 */
public class ManualSelectionConfiguration extends SelectionConfiguration {

    public ManualSelectionConfiguration(final InputFilter<DataColumnSpec> filter, final ConfigurationDialogFactory fac) {
        super(filter, fac);
    }

    /**
     * @param configurationDialogFactory
     */
    public ManualSelectionConfiguration(final ConfigurationDialogFactory fac) {
        super(fac);
    }

    @Override
    public int drop(final String s) {

        PaneConfigurationDialog dp = m_fac.getNewInstance();
        DropPaneConfig dpc = new DropPaneConfig();
        dpc.setPosition(m_index++);
        dpc.getSelection().addAll(Arrays.asList(s.split("\n")));
        dpc.setDialog(dp);
        m_panelList.put(dpc.getPosition(), dpc);

        return m_index - 1;
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        List<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
        int size = 0;

        try {
            size = settings.getInt("numberOfPanels");
        } catch (InvalidSettingsException e1) {
            // TODO Auto-generated catch block
        }
        List<String> inputList = new ArrayList<String>();
        if (m_filter != null) {
            for (DataColumnSpec s : specs[0]) {
                if (m_filter.include(s)) {
                    inputList.add(s.getName());
                }
            }
        } else {
            inputList.addAll(Arrays.asList(specs[0].getColumnNames()));
        }

        for (int i = 0; i < size; i++) {
            try {
                data.add(new ArrayList<String>(Arrays.asList(settings.getString("panelIndex_" + i).split("\n"))));

            } catch (InvalidSettingsException e) {
                // TODO Auto-generated catch block
            }
        }

        for (int i = 0; i < data.size(); i++) {
            ArrayList<String> list = data.get(i);
            for (int j = 0; j < list.size(); j++) {
                String string = list.get(j);
                if (!inputList.remove(string)) {
                    // append one space (column names never end with whitespace --> unique char)
                    string += " ";
                    list.set(j, string);
                }
            }

            data.set(i, list);
        }

        for (int i = 0; i < data.size(); i++) {
            PaneConfigurationDialog dp = m_fac.getNewInstance();
            NodeSettingsRO n;
            try {
                n = settings.getNodeSettings("dialogSettings_" + i);
                dp.loadSettingsFrom(n, specs);
            } catch (InvalidSettingsException e) {
                // TODO Auto-generated catch block
            }

            DropPaneConfig dpc = new DropPaneConfig();
            dpc.getSelection().addAll(data.get(i));
            dpc.setDialog(dp);
            dpc.setPosition(i);
            m_panelList.put(dpc.getPosition(), dpc);
        }
        for (int i = 0; i < inputList.size(); i++) {
            m_inputListModel.addElement(inputList.get(i));
        }
        m_index = m_panelList.size();

    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        int size = settings.getInt("numberOfPanels");

        for (int i = 0; i < size; i++) {
            DropPaneConfig dpc = new DropPaneConfig();
            dpc.getSelection().addAll(Arrays.asList(settings.getString("panelIndex_" + i).split("\n")));
            PaneConfigurationDialog dp = m_fac.getNewInstance();
            NodeSettingsRO n = settings.getNodeSettings("dialogSettings_" + i);

            dp.validateSettings(n);
            dpc.setDialog(dp);
            dpc.setPosition(i);
        }
    }

    /**
     * @param settings asdf
     * @throws InvalidSettingsException asdf
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        int size = settings.getInt("numberOfPanels");

        for (int i = 0; i < size; i++) {
            DropPaneConfig dpc = new DropPaneConfig();
            dpc.getSelection().addAll(Arrays.asList(settings.getString("panelIndex_" + i).split("\n")));
            PaneConfigurationDialog dp = m_fac.getNewInstance();
            NodeSettingsRO n = settings.getNodeSettings("dialogSettings_" + i);

            dp.loadValidatedSettings(n);
            dpc.setDialog(dp);
            dpc.setPosition(i);
            m_panelList.put(dpc.getPosition(), dpc);
        }

    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        settings.addInt("numberOfPanels", m_panelList.size());
        List<DropPaneConfig> values =
            Arrays.asList(m_panelList.values().toArray(new DropPaneConfig[m_panelList.size()]));
        Collections.sort(values);

        for (int i = 0; i < values.size(); i++) {
            DropPaneConfig dpc = values.get(i);
            PaneConfigurationDialog p = dpc.getDialog();
            NodeSettings n = new NodeSettings("dialogSettings_" + i);
            p.saveSettingsTo(n);
            settings.addNodeSettings(n);

            settings.addString("panelIndex_" + i, dpc.getSelectionAsString());

            //settings.addInt("panelIndex" + i, p.getIndex());
        }
    }

    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addInt("numberOfPanels", m_panelList.size());
        List<DropPaneConfig> values =
            Arrays.asList(m_panelList.values().toArray(new DropPaneConfig[m_panelList.size()]));
        Collections.sort(values);

        for (int i = 0; i < values.size(); i++) {
            DropPaneConfig dpc = values.get(i);
            PaneConfigurationDialog p = dpc.getDialog();
            NodeSettings n = new NodeSettings("dialogSettings_" + i);
            p.saveSettings(n);
            settings.addNodeSettings(n);

            settings.addString("panelIndex_" + i, dpc.getSelectionAsString());

            //settings.addInt("panelIndex" + i, p.getIndex());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pair<String, PaneConfigurationDialog>> configure(final DataTableSpec spec) {
        List<Pair<String, PaneConfigurationDialog>> r = new ArrayList<Pair<String, PaneConfigurationDialog>>();
        for (DropPaneConfig dpc : m_panelList.values()) {
            for (String str : dpc.getSelection()) {
                r.add(new Pair<String, PaneConfigurationDialog>(str, dpc.getDialog()));
            }
        }
        return r;
    }
}
