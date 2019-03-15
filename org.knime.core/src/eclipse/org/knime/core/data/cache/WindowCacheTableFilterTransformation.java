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
 *   12 Mar 2019 (albrecht): created
 */
package org.knime.core.data.cache;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DirectAccessTable;
import org.knime.core.data.transform.DataTableFilterInformation;
import org.knime.core.data.transform.TableFilterTransformation;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 *
 * @noextend This class is not intended to be subclassed by clients. Pending API
 * @noinstantiate This class is not intended to be instantiated by clients. Pending API
 * @noreference This class is not intended to be referenced by clients. Pending API
 */
public class WindowCacheTableFilterTransformation extends TableFilterTransformation {

    /**
     *
     */
    public WindowCacheTableFilterTransformation(final DataTableFilterInformation filterInfo) {
        super(filterInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectAccessTable transform(final DirectAccessTable originalTable, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        if (!(originalTable instanceof WindowCacheTable)) {
            throw new IllegalArgumentException("WindowCacheTable expected");
        }
        if (exec != null) {
            exec.checkCanceled();
            exec.setMessage("Filtering...");
            exec.setProgress(0);
        }
        WindowCacheTable cache = (WindowCacheTable)originalTable;
        List<DataTableFilterInformation> newFilters = new ArrayList<DataTableFilterInformation>();
        List<DataTableFilterInformation> filters = cache.getFilters();
        newFilters.addAll(filters);
        newFilters.add((DataTableFilterInformation)getFilter());
        WindowCacheTable newTable = new WindowCacheTable(cache.getDataTable(), newFilters,
            cache.getIncludedColumns().stream().toArray(String[]::new));
        int cacheSize = cache.getCacheSize();
        if (cacheSize != WindowCacheTable.DEFAULT_CACHE_SIZE) {
            newTable.setCacheSize(cacheSize);
        }
        int lookAhead = cache.getLookAheadSize();
        if (lookAhead != WindowCacheTable.DEFAULT_LOOK_AHEAD) {
            newTable.setLookAheadSize(cacheSize);
        }
        if (exec != null) {
            exec.setProgress(1);
        }
        return newTable;
    }

}
