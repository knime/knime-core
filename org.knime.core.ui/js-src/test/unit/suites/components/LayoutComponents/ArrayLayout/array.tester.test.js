import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';
import { arrayLayoutTester } from '@/components/renderers/arrayLayoutRenderer';

it('uses array layout', () => {
    expect(
        arrayLayoutTester({
            type: 'Section',
            label: 'Reference lines',
            scope: '#/properties/view/properties/referenceLines',
            elements: [
            ]
        },
        dialogInitialData.schema)
    ).toEqual(true);

    expect(
        arrayLayoutTester({
            type: 'Group',
            label: 'Data'
        },
        dialogInitialData.schema)
    ).toEqual(false);

    expect(
        arrayLayoutTester({
            type: 'Control',
            scope: '#/properties/view/properties/xAxisColumn'
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
