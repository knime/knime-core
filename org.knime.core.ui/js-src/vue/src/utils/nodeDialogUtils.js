export const optionsMapper = ({ const: id, title: text }) => ({ id, text });

export const checkIsModelSetting = (control) => control?.uischema?.scope?.includes('model');

// creates an object which maps the path of each element to their flow variable object
export const createFlowVariablesMap = ({ viewVariables, modelVariables } = {}) => {
    const traverseObject = (variables, currentPath, initialValue = {}) => {
        if (!variables) {
            return {};
        }

        return Object.entries(variables).reduce((acc, [key, value]) => {
            if (value.leaf) {
                acc[currentPath ? `${currentPath}.${key}` : key] = value;
                return acc;
            }

            // TODO: Remove key === 'value' check when it's removed from backend implementation.
            return traverseObject(value, `${currentPath}${key === 'value' ? '' : `.${key}`}`, acc);
        }, initialValue);
    };

    return { ...traverseObject(viewVariables, 'view'),
        ...traverseObject(modelVariables, 'model') };
};

// expects a modified root schema which includes a flowVariablesMap
// the root schema should look like the following:
// rootSchema: {
//    flowVariablesMap: {},
//    schema: {},
//    uischema: {},
//    ...
// }
export const getFlowVariablesMap = ({ rootSchema, path }) => rootSchema?.flowVariablesMap
    ? rootSchema.flowVariablesMap[path]
    : null;
