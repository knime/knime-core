import { valueSwitchTester } from '@/components/renderers/valueSwitchRenderer';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

it('check the switch tester will match a requested value switch input', () => {
    expect(
        valueSwitchTester({
            type: 'Control',
            scope: '#/properties/view/properties/yAxisScale',
            options: {
                format: 'valueSwitch'
            }
        },
        dialogInitialData.schema)
    ).toEqual(true);
});

it('check the switch tester won\'t match other requested formats', () => {
    expect(
        valueSwitchTester({
            type: 'Control',
            scope: '#/properties/view/properties/yAxisScale',
            options: {
                format: 'columnSelection'
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);
});

it('check the switch tester won\'t match non-control types', () => {
    expect(
        valueSwitchTester({
            type: 'Section',
            scope: '#/properties/view/properties/yAxisScale',
            options: {
                format: 'valueSwitch'
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);
});

it('check the switch tester won\'t match wrong type and format', () => {
    expect(
        valueSwitchTester({
            type: 'Section',
            scope: '#/properties/view/properties/yAxisScale',
            options: {
                format: 'columnSelect'
            }
        },
        dialogInitialData.schema)
    ).toEqual(false);
});
