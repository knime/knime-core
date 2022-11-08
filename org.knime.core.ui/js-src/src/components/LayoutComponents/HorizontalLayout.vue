<script>
import { defineComponent } from '@vue/composition-api';
import { useJsonFormsLayout, rendererProps, DispatchRenderer } from '@jsonforms/vue2';

const HorizontalLayout = defineComponent({
    name: 'HorizontalLayout',
    components: {
        DispatchRenderer
    },
    props: {
        ...rendererProps()
    },
    setup(props) {
        return useJsonFormsLayout(props);
    }
});
export default HorizontalLayout;
</script>

<template>
  <div class="horizontal">
    <div
      v-for="(element, index) in layout.uischema.elements"
      :key="`${layout.path}-${index}`"
    >
      <DispatchRenderer
        :schema="layout.schema"
        :uischema="element"
        :path="layout.path"
        :enabled="layout.enabled"
        :renderers="layout.renderers"
        :cells="layout.cells"
      />
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.horizontal {
  display: grid;
  grid-template-columns: 70px 120px;
  grid-column-gap: 10px;
}

</style>
