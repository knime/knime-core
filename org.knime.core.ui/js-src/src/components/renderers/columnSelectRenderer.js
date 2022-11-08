import { rankWith, isOneOfControl } from '@jsonforms/core';
import ColumnSelect from '@/components/UIComponents/ColumnSelect.vue';
import { priorityRanks, inputFormats } from '@/constants';

export const columnSelectTester = (uischema, schema) => {
    const isOneOf = isOneOfControl(uischema, schema);
    return isOneOf && uischema.options?.format === inputFormats.columnSelect;
};

export const columnSelectRenderer = {
    renderer: ColumnSelect,
    tester: rankWith(priorityRanks.default, columnSelectTester)
};
