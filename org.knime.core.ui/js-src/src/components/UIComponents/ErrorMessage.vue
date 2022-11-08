<script>
export default {
    props: {
        errors: {
            default: () => [],
            type: Array
        }
    },
    computed: {
        displayableErrors() {
            return this.errors.map(({ message }) => message)?.join('; ');
        }
    }
};
</script>

<template>
  <div :title="displayableErrors">{{ displayableErrors }}</div>
</template>

<style lang="postcss" scoped>
div {
  display: none;
  font-weight: 400;
  font-size: 12px;
  line-height: 13px;
  color: var(--theme-color-kudos);
  position: relative;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
