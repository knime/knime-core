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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.util.createtempdir;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class CreateTempDirectoryConfiguration {

    private String m_baseName;
    private String m_variableName;
    private boolean m_deleteOnReset;
    private VarNameFileNamePair[] m_pairs;

    /** @return the baseName */
    String getBaseName() {
        return m_baseName;
    }

    /** @param baseName the baseName to set */
    void setBaseName(final String baseName) {
        m_baseName = baseName;
    }

    /** @return the deleteOnReset */
    boolean isDeleteOnReset() {
        return m_deleteOnReset;
    }

    /** @param deleteOnReset the deleteOnReset to set */
    void setDeleteOnReset(final boolean deleteOnReset) {
        m_deleteOnReset = deleteOnReset;
    }
    /** @return the variableName */
    String getVariableName() {
        return m_variableName;
    }

    /** @param variableName the variableName to set */
    void setVariableName(final String variableName) {
        m_variableName = variableName;
    }

    /** @return the pairs */
    VarNameFileNamePair[] getPairs() {
        return m_pairs;
    }

    /** @param pairs the pairs to set */
    void setPairs(final VarNameFileNamePair[] pairs) {
        m_pairs = pairs;
    }

    void save(final NodeSettingsWO settings) {
        settings.addString("baseName", m_baseName);
        settings.addString("variableName", m_variableName);
        settings.addBoolean("deleteOnReset", m_deleteOnReset);
        NodeSettingsWO pairs = settings.addNodeSettings("variable_name_pairs");
        if (m_pairs != null) {
            for (VarNameFileNamePair v : m_pairs) {
                NodeSettingsWO p = pairs.addNodeSettings(v.getVariableName());
                p.addString("variableName", v.getVariableName());
                p.addString("fileName", v.getFileName());
            }
        }
    }

    void loadInDialog(final NodeSettingsRO settings) {
        m_baseName = settings.getString("baseName", "knime_tempcreator_");
        m_variableName = settings.getString("variableName", "temp_path");
        m_deleteOnReset = settings.getBoolean("deleteOnReset", true);
        NodeSettingsRO pairs;
        try {
            pairs = settings.getNodeSettings("variable_name_pairs");
        } catch (InvalidSettingsException ise) {
            pairs = new NodeSettings("empty");
        }
        Set<String> keySet = pairs.keySet();
        List<VarNameFileNamePair> pairList =
            new ArrayList<VarNameFileNamePair>();
        m_pairs = new VarNameFileNamePair[keySet.size()];
        for (String key : keySet) {
            try {
                NodeSettingsRO p = pairs.getNodeSettings(key);
                String varName = p.getString("variableName");
                String fileName = p.getString("fileName");
                if (varName != null && fileName != null) {
                    pairList.add(new VarNameFileNamePair(varName, fileName));
                }
            } catch (InvalidSettingsException ise) {
                // ignore
            }
        }
        m_pairs = pairList.toArray(new VarNameFileNamePair[pairList.size()]);
    }

    void loadInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_baseName = settings.getString("baseName");
        if (m_baseName == null || m_baseName.length() == 0) {
            throw new InvalidSettingsException("Invalid (empty) temp "
                    + "directory base name");
        }
        m_variableName = settings.getString("variableName");
        if (m_variableName == null || m_variableName.length() == 0) {
            throw new InvalidSettingsException("Invalid (empty) variable name");
        }
        m_deleteOnReset = settings.getBoolean("deleteOnReset");

        NodeSettingsRO pairs = settings.getNodeSettings("variable_name_pairs");
        Set<String> keySet = pairs.keySet();
        List<VarNameFileNamePair> pairList =
            new ArrayList<VarNameFileNamePair>();
        m_pairs = new VarNameFileNamePair[keySet.size()];
        for (String key : keySet) {
            NodeSettingsRO p = pairs.getNodeSettings(key);
            String varName = p.getString("variableName");
            String fileName = p.getString("fileName");
            pairList.add(new VarNameFileNamePair(varName, fileName));
        }
        m_pairs = pairList.toArray(new VarNameFileNamePair[pairList.size()]);
    }


    static class VarNameFileNamePair {
        private final String m_variableName;
        private final String m_fileName;


        /**
         * @param variableName
         * @param fileName
         */
        public VarNameFileNamePair(
                final String variableName, final String fileName) {
            m_variableName = variableName;
            m_fileName = fileName;
        }
        /** @return the variableName */
        String getVariableName() {
            return m_variableName;
        }
        /** @return the fileName */
        String getFileName() {
            return m_fileName;
        }

    }

}
