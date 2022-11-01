import { rankWith, isAnyOfControl } from '@jsonforms/core';
import ColumnFilter from '@/components/UIComponents/ColumnFilter.vue';
import { priorityRanks, inputFormats } from '@/constants';

export const columnFilterTester = (uischema, schema) => {
    const isAnyOf = isAnyOfControl(uischema, schema);
    return isAnyOf && uischema.options?.format === inputFormats.columnFilter;
};

export const columnFilterRenderer = {
    renderer: ColumnFilter,
    tester: rankWith(priorityRanks.default, columnFilterTester)
};
