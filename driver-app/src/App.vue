<script>
export default {
  onLaunch() {
    // 冷启动：校验本地令牌，过期则回登录页（避免进首页再被 401 打断）
    const token = uni.getStorageSync('driver_token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/login' })
    }
  }
}
</script>

<style>
/* 全局样式：司机在车上单手操作，字号比常规后台放大一档，按钮最小 88rpx 高 */
page {
  background-color: #f4f6fa;
  color: #1f2733;
  font-size: 30rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}

.card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin: 20rpx 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(23, 43, 77, 0.06);
}

.row {
  display: flex;
  align-items: center;
}

.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.muted {
  color: #8a94a6;
  font-size: 26rpx;
}

.strong {
  font-weight: 600;
}

.mono {
  font-family: 'SF Mono', Consolas, Monaco, monospace;
}

.btn-primary {
  background: linear-gradient(135deg, #2f6bff, #1a4fdc);
  color: #ffffff;
  border-radius: 16rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
}

.btn-primary[disabled] {
  background: #c8d2e4;
  color: #ffffff;
}

.btn-ghost {
  border: 2rpx solid #2f6bff;
  color: #2f6bff;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 84rpx;
  text-align: center;
  font-size: 30rpx;
  background: #ffffff;
}

.btn-plain {
  border: 2rpx solid #d9e0ec;
  color: #48546b;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 84rpx;
  text-align: center;
  font-size: 30rpx;
  background: #ffffff;
}

.divider {
  height: 2rpx;
  background: #eef2f8;
  margin: 20rpx 0;
}
</style>
