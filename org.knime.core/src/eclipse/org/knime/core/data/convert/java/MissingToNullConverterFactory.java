package org.knime.core.data.convert.java;

import org.knime.core.data.MissingValue;

/**
 * Collection converter factories may need to convert a MissingValue into some java object.
 *
 * @author Jonathan Hale
 * @since 3.2
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class MissingToNullConverterFactory implements DataCellToJavaConverterFactory<MissingValue, Object> {

    private static final MissingToNullConverterFactory m_instance = new MissingToNullConverterFactory();

    private MissingToNullConverterFactory() {
    }

    @Override
    public DataCellToJavaConverter<MissingValue, Object> create() {
        return (v) -> null;
    }

    @Override
    public Class<MissingValue> getSourceType() {
        return MissingValue.class;
    }

    @Override
    public Class<Object> getDestinationType() {
        return Object.class;
    }

    /**
     * @return instance of this singleton
     */
    public static MissingToNullConverterFactory getInstance() {
        return m_instance;
    }

    @Override
    public String getIdentifier() {
        return "missing";
    }

}
