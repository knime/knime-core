import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';
import { inputFormats } from '@/constants/inputFormats';
import { checkboxTester } from '@/components/renderers/checkboxRenderer';

it('checkboxTester', () => {
    expect(
        checkboxTester({
            type: 'Control',
            scope: '#/properties/view/properties/showTooltip',
            options: {
                format: inputFormats.checkbox
            }
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        checkboxTester({
            type: 'Control',
            scope: '#/properties/view/properties/showTooltip'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        checkboxTester({
            type: 'Section',
            scope: '#/properties/view/properties/showTooltip',
            options: {
                format: inputFormats.checkbox
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        checkboxTester({
            type: 'Section',
            scope: '#/properties/view/properties/showTooltip'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
