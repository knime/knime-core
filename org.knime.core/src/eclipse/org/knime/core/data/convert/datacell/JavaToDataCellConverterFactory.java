package org.knime.core.data.convert.datacell;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.node.ExecutionContext;

/**
 * Interface for all factory classes which create {@link JavaToDataCellConverter JavaToDataCellConverters}.
 *
 * You may extend this class and register the derived class under the extension point
 * "org.knime.core.JavaToDataCellConverter". It is recommended that you extend
 * {@link SimpleJavaToDataCellConverterFactory} if possible.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 *
 * @param <S> Java type which can be converted by the {@link JavaToDataCellConverter}s created by this factory
 * @since 3.2
 *
 * @see JavaToDataCellConverter
 * @see JavaToDataCellConverterRegistry
 */
public interface JavaToDataCellConverterFactory<S> {

    /**
     * Create a {@link JavaToDataCellConverter}
     *
     * @param context {@link ExecutionContext} which may be used for creating {@link CellFactory}s.
     * @return a {@link JavaToDataCellConverter} instances
     */
    public JavaToDataCellConverter<S> create(final ExecutionContext context);

    /**
     * @return DataType of the {@link DataCell} created by the {@link JavaToDataCellConverter}s produced by this
     *         factory.
     */
    public DataType getDestinationType();

    /**
     * @return type which the created {@link DataCellToJavaConverter}s can convert
     */
    public Class<S> getSourceType();

    /**
     * @return the name of this converter factory or <code>""</code> if this is the default for the source and
     *         destination types
     */
    default String getName() {
        return "";
    }

    /**
     * @return a ideally unique identifier for this factory
     */
    public String getIdentifier();
}