import { numberTester } from '@/components/renderers/numberRenderer';
import { inputFormats } from '@/constants/inputFormats';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

it('numberTester', () => {
    expect(
        numberTester({
            type: 'Control',
            scope: '#/properties/view/properties/fraction',
            options: {
                format: inputFormats.number
            }
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        numberTester({
            type: 'Control',
            scope: '#/properties/view/properties/fraction'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        numberTester({
            type: 'Section',
            scope: '#/properties/view/properties/fraction',
            options: {
                format: inputFormats.number
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        numberTester({
            type: 'Section',
            scope: '#/properties/view/properties/fraction'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
