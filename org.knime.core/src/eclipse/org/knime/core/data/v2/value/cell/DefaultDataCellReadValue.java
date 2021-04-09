package org.knime.core.data.v2.value.cell;

import org.knime.core.data.DataCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;

final class DefaultDataCellReadValue implements ReadValue {

    private final ObjectReadAccess<DataCell> m_access;

    DefaultDataCellReadValue(final ObjectReadAccess<DataCell> access) {
        m_access = access;
    }

    @Override
    public boolean isMissing() {
        return m_access.isMissing();
    }

    @Override
    public DataCell getDataCell() {
        return m_access.getObject();
    }
}