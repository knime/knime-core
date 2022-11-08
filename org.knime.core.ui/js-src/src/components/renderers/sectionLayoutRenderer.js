import { rankWith, uiTypeIs } from '@jsonforms/core';
import SectionLayout from '@/components/LayoutComponents/SectionLayout.vue';
import { priorityRanks } from '@/constants';

export const sectionLayoutTester = uiTypeIs('Section');

export const sectionLayoutRenderer = {
    renderer: SectionLayout,
    tester: rankWith(priorityRanks.default, sectionLayoutTester)
};
