import { dropDownTester } from '@/components/renderers/dropdownRenderer';
import { inputFormats } from '@/constants/inputFormats';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

it('dropDownTester', () => {
    expect(
        dropDownTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn',
            options: {
                format: inputFormats.oneOfDropdown
            }
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        dropDownTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        dropDownTester({
            type: 'Section',
            scope: '#/properties/view/properties/xAxisColumn',
            options: {
                format: inputFormats.oneOfDropdown
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        dropDownTester({
            type: 'Section',
            scope: '#/properties/view/properties/xAxisColumn'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
