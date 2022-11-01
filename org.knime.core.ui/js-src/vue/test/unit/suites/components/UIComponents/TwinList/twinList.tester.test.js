import { twinlistTester } from '@/components/renderers/twinlistRenderer';
import { inputFormats } from '@/constants/inputFormats';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

it('twinlistTester', () => {
    expect(
        twinlistTester(
            {
                type: 'Control',
                scope: '#/properties/view/properties/frequencyColumns',
                options: {
                    format: inputFormats.anyOfTwinList
                }
            },
            dialogInitialData.schema
        )
    ).toEqual(true);

    expect(
        twinlistTester({
            type: 'Control',
            scope: '#/properties/view/properties/frequencyColumns'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        twinlistTester({
            type: 'Section',
            scope: '#/properties/view/properties/frequencyColumns',
            options: {
                format: inputFormats.anyOfTwinList
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        twinlistTester({
            type: 'Section',
            scope: '#/properties/view/properties/frequencyColumns'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
