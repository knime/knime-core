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
 * ---------------------------------------------------------------------
 *
 * History
 *   07.09.2011 (meinl): created
 */
package org.knime.testing.node.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestConfigSettings {
    private String m_owner;

    private List<String> m_requiredLogErrors = new ArrayList<String>();

    private List<String> m_requiredLogWarnings = new ArrayList<String>();

    private List<String> m_requiredLogInfos = new ArrayList<String>();

    private List<String> m_requiredLogDebugs = new ArrayList<String>();

    private List<String> m_failingNodes = new ArrayList<String>();

    private List<String> m_requiredNodeWarnings = new ArrayList<String>();

    private static final String[] EMPTY = new String[0];

    public String owner() {
        return m_owner;
    }

    public void owner(final String s) {
        m_owner = s;
    }

    public List<String> requiredLogErrors() {
        return Collections.unmodifiableList(m_requiredLogErrors);
    }

    public List<String> requiredLogWarnings() {
        return Collections.unmodifiableList(m_requiredLogWarnings);
    }

    public List<String> requiredLogInfos() {
        return Collections.unmodifiableList(m_requiredLogInfos);
    }

    public List<String> requiredLogDebugs() {
        return Collections.unmodifiableList(m_requiredLogDebugs);
    }

    public List<String> failingNodes() {
        return Collections.unmodifiableList(m_failingNodes);
    }

    public List<String> requiredNodeWarnings() {
        return Collections.unmodifiableList(m_requiredNodeWarnings);
    }

    public void requiredLogErrors(final Collection<String> col) {
        m_requiredLogErrors.clear();
        m_requiredLogErrors.addAll(col);
    }

    public void requiredLogWarnings(final Collection<String> col) {
        m_requiredLogWarnings.clear();
        m_requiredLogWarnings.addAll(col);
    }

    public void requiredLogInfos(final Collection<String> col) {
        m_requiredLogInfos.clear();
        m_requiredLogInfos.addAll(col);
    }

    public void requiredLogDebugs(final Collection<String> col) {
        m_requiredLogDebugs.clear();
        m_requiredLogDebugs.addAll(col);
    }

    public void failingNodes(final Collection<String> col) {
        m_failingNodes.clear();
        m_failingNodes.addAll(col);
    }

    public void requiredNodeWarnings(final Collection<String> col) {
        m_requiredNodeWarnings.clear();
        m_requiredNodeWarnings.addAll(col);
    }

    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_owner = settings.getString("owner");

        String[] temp = settings.getStringArray("requiredLogErrors");
        m_requiredLogErrors.clear();
        for (String s : temp) {
            m_requiredLogErrors.add(s);
        }

        temp = settings.getStringArray("requiredLogWarnings");
        m_requiredLogWarnings.clear();
        for (String s : temp) {
            m_requiredLogWarnings.add(s);
        }

        temp = settings.getStringArray("requiredLogInfos");
        m_requiredLogInfos.clear();
        for (String s : temp) {
            m_requiredLogInfos.add(s);
        }

        temp = settings.getStringArray("requiredLogDebugs");
        m_requiredLogDebugs.clear();
        for (String s : temp) {
            m_requiredLogDebugs.add(s);
        }

        temp = settings.getStringArray("failingNodes");
        m_failingNodes.clear();
        for (String s : temp) {
            m_failingNodes.add(s);
        }

        temp = settings.getStringArray("requiredNodeWarnings");
        m_requiredNodeWarnings.clear();
        for (String s : temp) {
            m_requiredNodeWarnings.add(s);
        }
    }

    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_owner = settings.getString("owner", null);
        if (m_owner == null) {
            m_owner = System.getProperty("user.name") + "@inf.uni-konstanz.de";
        }

        String[] temp = settings.getStringArray("requiredLogErrors", EMPTY);
        m_requiredLogErrors.clear();
        for (String s : temp) {
            m_requiredLogErrors.add(s);
        }

        temp = settings.getStringArray("requiredLogWarnings", EMPTY);
        m_requiredLogWarnings.clear();
        for (String s : temp) {
            m_requiredLogWarnings.add(s);
        }

        temp = settings.getStringArray("requiredLogInfos", EMPTY);
        m_requiredLogInfos.clear();
        for (String s : temp) {
            m_requiredLogInfos.add(s);
        }

        temp = settings.getStringArray("requiredLogDebugs", EMPTY);
        m_requiredLogDebugs.clear();
        for (String s : temp) {
            m_requiredLogDebugs.add(s);
        }

        temp = settings.getStringArray("failingNodes", EMPTY);
        m_failingNodes.clear();
        for (String s : temp) {
            m_failingNodes.add(s);
        }

        temp = settings.getStringArray("requiredNodeWarnings", EMPTY);
        m_requiredNodeWarnings.clear();
        for (String s : temp) {
            m_requiredNodeWarnings.add(s);
        }
    }

    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("owner", m_owner);
        settings.addStringArray("requiredLogErrors",
                m_requiredLogErrors.toArray(EMPTY));
        settings.addStringArray("requiredLogWarnings",
                m_requiredLogWarnings.toArray(EMPTY));
        settings.addStringArray("requiredLogInfos",
                m_requiredLogInfos.toArray(EMPTY));
        settings.addStringArray("requiredLogDebugs",
                m_requiredLogDebugs.toArray(EMPTY));
        settings.addStringArray("failingNodes", m_failingNodes.toArray(EMPTY));
        settings.addStringArray("requiredNodeWarnings",
                m_requiredNodeWarnings.toArray(EMPTY));
    }
}
