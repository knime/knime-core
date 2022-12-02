import { optionsMapper, createFlowVariablesMap, isModelSettingAndHasNodeView } from '@/utils/nodeDialogUtils';

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

    test('isModelSettingsAndhasNodeView', () => {
        const control = {
            rootSchema: {
                hasNodeView: true
            },
            uischema: {
                scope: '#/properties/model/blub'
            }


        };
        expect(isModelSettingAndHasNodeView(control)).toBeTruthy();

        control.rootSchema.hasNodeView = false;
        expect(isModelSettingAndHasNodeView(control)).toBeFalsy();

        control.rootSchema.hasNodeView = true;
        control.uischema.scope = '#/properties/view/blub';
        expect(isModelSettingAndHasNodeView(control)).toBeFalsy();
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
