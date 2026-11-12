<template>
  <view class="tabbar">
    <view
      v-for="item in items"
      :key="item.key"
      class="tab"
      :class="{ active: item.key === current }"
      @click="go(item)"
    >
      <view class="tab-dot" :class="{ on: item.key === current }">
        <text v-if="item.badge > 0" class="badge">{{ item.badge > 99 ? '99+' : item.badge }}</text>
      </view>
      <text class="tab-text">{{ item.label }}</text>
    </view>
  </view>
  <view class="tabbar-holder" />
</template>

<script setup>
/**
 * 自定义底部导航。
 *
 * 不用原生 tabBar：原生 tabBar 需要 4 组 png 图标（普通/选中 ×2），
 * 一次改色就要重新出图；这里用纯 CSS 圆点 + 文案，配色跟着主题变量走，
 * 后续换品牌色只改一处。
 */
const props = defineProps({
  current: { type: String, default: 'home' },
  badges: { type: Object, default: () => ({}) }
})

const items = [
  { key: 'home', label: '工作台', url: '/pages/index/index' },
  { key: 'hall', label: '货源大厅', url: '/pages/hall/hall' },
  { key: 'waybill', label: '我的运单', url: '/pages/waybill/list' },
  { key: 'mine', label: '我的', url: '/pages/mine/mine' }
].map((i) => ({ ...i, badge: props.badges[i.key] || 0 }))

function go(item) {
  if (item.key === props.current) return
  uni.reLaunch({ url: item.url })
}
</script>

<style scoped>
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  background: #ffffff;
  border-top: 2rpx solid #eef2f8;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 99;
}

.tab {
  flex: 1;
  height: 108rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.tab-dot {
  position: relative;
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: #cfd8e6;
  margin-bottom: 8rpx;
}

.tab-dot.on {
  width: 34rpx;
  border-radius: 10rpx;
  background: #2f6bff;
}

.tab-text {
  font-size: 22rpx;
  color: #8a94a6;
}

.tab.active .tab-text {
  color: #2f6bff;
  font-weight: 600;
}

.badge {
  position: absolute;
  top: -12rpx;
  left: 20rpx;
  min-width: 26rpx;
  height: 26rpx;
  padding: 0 6rpx;
  border-radius: 13rpx;
  background: #d9363e;
  color: #ffffff;
  font-size: 18rpx;
  line-height: 26rpx;
  text-align: center;
}

.tabbar-holder {
  height: 120rpx;
}
</style>
