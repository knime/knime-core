/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Sep 10, 2018 (hornm): created
 */
package org.knime.core.node.execenv;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.node.execenv.dummies.a.AExecEnvFactory;
import org.knime.core.node.execenv.dummies.b.BExecEnvFactory;
import org.knime.core.node.execenv.dummies.c.CExecEnvFactory;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * Execution environment will need to be persisted.
 *
 * @author hornm
 * @since 3.7
 */
public class ExecEnvManager {

    private static ExecEnvManager INSTANCE;

    private Map<String, ExecEnvFactory> m_idToExecEnvMap = new HashMap<String, ExecEnvFactory>();

    private Map<Integer, ExecEnv> m_ncToExecEnvMap = new HashMap<Integer, ExecEnv>();

    private ExecEnvManager() {
        //singleton
        m_idToExecEnvMap.put("A", new AExecEnvFactory());
        m_idToExecEnvMap.put("B", new BExecEnvFactory());
        m_idToExecEnvMap.put("C", new CExecEnvFactory());
    }

    public static ExecEnvManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExecEnvManager();
        }
        return INSTANCE;
    }

    public Collection<ExecEnvFactory> getAvailableExecEnvFactories() {
        return m_idToExecEnvMap.values();
    }

    public ExecEnvFactory getExecEnvFactoryByID(final String id) {
        return m_idToExecEnvMap.get(id);
    }

    public void registerExecEnv(final ExecEnv ee, final NodeContainer nc) {
        m_ncToExecEnvMap.put(System.identityHashCode(nc), ee);
    }

    public void deregisterExecEnv(final SubNodeContainer snc) {
        m_ncToExecEnvMap.remove(System.identityHashCode(snc));
    }

    public List<ExecEnv> getRegisteredExecEnvsOfType(final String id) {
        return m_ncToExecEnvMap.values().stream().filter(ee -> id != null && id.equals(ee.getFactory().getExecEnvID()))
            .collect(Collectors.toList());
    }

}
