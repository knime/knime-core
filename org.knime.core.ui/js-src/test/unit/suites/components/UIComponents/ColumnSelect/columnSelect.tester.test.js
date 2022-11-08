import { columnSelectTester } from '@/components/renderers/columnSelectRenderer';
import { inputFormats } from '@/constants/inputFormats';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

it('columnSelectTester', () => {
    expect(
        columnSelectTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn',
            options: {
                format: inputFormats.columnSelect
            }
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        columnSelectTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn',
            options: {
                format: inputFormats.oneOfDropdown
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        columnSelectTester({
            type: 'Section',
            scope: '#/properties/view/properties/xAxisColumn',
            options: {
                format: inputFormats.columnSelect
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        columnSelectTester({
            type: 'Section',
            scope: '#/properties/view/properties/xAxisColumn',
            options: {
                format: inputFormats.oneOfDropdown
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
