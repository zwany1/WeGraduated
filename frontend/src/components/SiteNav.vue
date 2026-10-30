<template>
  <SiteHeader variant="workbench" back>
    <template #actions>
      <slot />
    </template>
  </SiteHeader>
</template>

<script setup>
import SiteHeader from './SiteHeader.vue'
</script>
