package org.knime.core.data.convert.java;

/**
 *
 * @author Jonathan Hale
 *
 * @param <S>
 *            Type which can be converted
 * @param <D>
 *            Type which can be converted to
 * @since 3.2
 */
@FunctionalInterface
public interface DataCellToJavaConverter<S, D> {

	/**
	 * Convert <code>source</code> into an instance of type <D>.
	 *
	 * @param source
	 *            Object to convert
	 * @return the converted object.
	 * @throws Exception
	 */
	public D convert(S source) throws Exception;
}