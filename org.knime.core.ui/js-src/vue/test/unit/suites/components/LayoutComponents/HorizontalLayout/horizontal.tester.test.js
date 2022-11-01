import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';
import { horizontalLayoutTester } from '@/components/renderers/horizontalLayoutRenderer';

it('uses horizontal layout', () => {
    expect(
        horizontalLayoutTester({
            type: 'HorizontalLayout'
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        horizontalLayoutTester({
            type: 'Section',
            label: 'Data'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        horizontalLayoutTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
