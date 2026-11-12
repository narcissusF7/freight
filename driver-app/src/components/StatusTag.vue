<template>
  <text class="tag" :style="{ color: color, background: color + '1a', borderColor: color + '33' }">
    {{ text }}
  </text>
</template>

<script setup>
import { computed } from 'vue'
import { statusText, statusColor } from '@/utils/format'

const props = defineProps({
  status: { type: String, default: '' }
})
const text = computed(() => statusText(props.status))
const color = computed(() => statusColor(props.status))
</script>

<style scoped>
.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  border: 2rpx solid transparent;
}
</style>
