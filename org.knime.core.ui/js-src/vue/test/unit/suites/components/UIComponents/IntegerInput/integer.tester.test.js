import { integerTester } from '@/components/renderers/integerRenderer';
import { inputFormats } from '@/constants/inputFormats';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

it('integerTester', () => {
    expect(
        integerTester({
            type: 'Control',
            scope: '#/properties/view/properties/maxRows',
            options: {
                format: inputFormats.integer
            }
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        integerTester({
            type: 'Control',
            scope: '#/properties/view/properties/maxRows'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        integerTester({
            type: 'Section',
            scope: '#/properties/view/properties/maxRows',
            options: {
                format: inputFormats.integer
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        integerTester({
            type: 'Section',
            scope: '#/properties/view/properties/maxRows'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
