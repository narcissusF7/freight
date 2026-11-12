<template>
  <view class="page">
    <view class="topbar between">
      <text class="muted">共 {{ list.length }} 条，未读 {{ unreadCount }} 条</text>
      <text class="link" @click="readAll">全部已读</text>
    </view>

    <view v-if="!list.length" class="card empty">
      <text class="empty-title">暂无消息</text>
      <text class="muted">接单成功、运单状态变更、在途预警都会在这里提醒</text>
    </view>

    <view
      v-for="m in list"
      :key="m.id"
      class="card msg"
      :class="{ unread: m.readFlag === 0 }"
      @click="open(m)"
    >
      <view class="between">
        <view class="row">
          <text v-if="m.readFlag === 0" class="dot" />
          <text class="msg-title">{{ m.title || '系统消息' }}</text>
        </view>
        <text class="muted">{{ shortTime(m.createTime) }}</text>
      </view>
      <text class="msg-content">{{ m.content }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import * as api from '@/api/driver'
import { shortTime } from '@/utils/format'

const list = ref([])
const unreadCount = computed(() => list.value.filter((m) => m.readFlag === 0).length)

onShow(load)

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function load() {
  try {
    list.value = await api.messages(false)
  } catch (e) {
    list.value = []
  }
}

async function open(m) {
  if (m.readFlag === 1) return
  try {
    await api.readMessages([m.id])
    m.readFlag = 1
  } catch (e) {
    // 已提示
  }
}

async function readAll() {
  const ids = list.value.filter((m) => m.readFlag === 0).map((m) => m.id)
  if (!ids.length) {
    uni.showToast({ title: '没有未读消息', icon: 'none' })
    return
  }
  try {
    await api.readMessages(ids)
    await load()
    uni.showToast({ title: '已全部标记为已读', icon: 'none' })
  } catch (e) {
    // 已提示
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.topbar {
  background: #ffffff;
  padding: 28rpx 28rpx;
  border-bottom: 2rpx solid #eef2f8;
}

.link {
  font-size: 26rpx;
  color: #2f6bff;
}

.empty {
  text-align: center;
  padding: 60rpx 28rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.msg {
  padding: 24rpx 28rpx;
}

.msg.unread {
  border-left: 8rpx solid #2f6bff;
}

.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #d9363e;
  margin-right: 12rpx;
}

.msg-title {
  font-size: 30rpx;
  font-weight: 600;
}

.msg-content {
  display: block;
  font-size: 27rpx;
  color: #48546b;
  line-height: 1.6;
  margin-top: 12rpx;
}
</style>
