import { textTester } from '@/components/renderers/textRenderer';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

it('textTester', () => {
    expect(
        textTester(
            {
                type: 'Control',
                scope: '#/properties/view/properties/xAxisLabel'
            },
            dialogInitialData.schema
        )
    ).toEqual(true);

    expect(
        textTester({
            type: 'Section',
            scope: '#/properties/view/properties/xAxisLabel'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        textTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn',
            options: {
                format: 'columnSelection'
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
