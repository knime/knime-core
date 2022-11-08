import { optionsMapper, createFlowVariablesMap } from '@/utils/nodeDialogUtils';

describe('Utils', () => {
    it('optionsMapper maps Knime row data presentation to echarts index value', () => {
        expect(
            [
                { const: 'rowName', title: 'Row Name' },
                { const: 'columName', title: 'Colum Name' }
            ].map(optionsMapper)
        ).toEqual([
            { id: 'rowName', text: 'Row Name' },
            { id: 'columName', text: 'Colum Name' }
        ]);
    });

    it('createFlowVariablesMap maps flowVariables correctly', () => {
        const viewVariables = {
            test: {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariablename: 'test',
                leaf: true
            }
        };
        const modelVariables = {
            test: {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariablename: 'test',
                leaf: true
            }
        };
        const expectedResult = {
            'view.test': {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariablename: 'test',
                leaf: true
            },
            'model.test': {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariablename: 'test',
                leaf: true
            }
        };
        expect(createFlowVariablesMap({ viewVariables, modelVariables })).toEqual(expectedResult);
    });
});
