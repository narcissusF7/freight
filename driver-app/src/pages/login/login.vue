<template>
  <view class="page">
    <view class="hero">
      <view class="brand">鹭卡畅通</view>
      <view class="slogan">司机端 · 找货接单 · 节点确认</view>
    </view>

    <view class="panel">
      <view class="panel-title">{{ mode === 'login' ? '登录' : '开通司机端账号' }}</view>

      <view class="field">
        <text class="label">手机号</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          maxlength="11"
          placeholder="请输入登记的手机号"
          placeholder-class="ph"
        />
      </view>

      <view v-if="mode === 'login'" class="field">
        <text class="label">密码</text>
        <input
          v-model="form.password"
          class="input"
          password
          placeholder="请输入密码"
          placeholder-class="ph"
        />
      </view>

      <template v-else>
        <view class="field">
          <text class="label">验证码</text>
          <view class="row">
            <input
              v-model="form.smsCode"
              class="input flex1"
              type="number"
              maxlength="6"
              placeholder="6 位短信验证码"
              placeholder-class="ph"
            />
            <view class="code-btn" @click="sendCode">{{ codeText }}</view>
          </view>
        </view>
        <view class="field">
          <text class="label">新密码</text>
          <input
            v-model="form.newPassword"
            class="input"
            password
            placeholder="至少 6 位"
            placeholder-class="ph"
          />
        </view>
      </template>

      <view v-if="error" class="error">{{ error }}</view>

      <view class="btn-primary" :class="{ loading }" @click="submit">
        {{ loading ? '请稍候…' : mode === 'login' ? '登录' : '开通并登录' }}
      </view>

      <view class="switch" @click="toggleMode">
        {{ mode === 'login' ? '首次使用？开通司机端账号' : '已有账号，直接登录' }}
      </view>
    </view>

    <view class="footer">
      <text class="muted">登录即表示同意《运输服务协议》与《隐私政策》</text>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import * as api from '@/api/driver'
import { setSession } from '@/store/auth'

const mode = ref('login')
const loading = ref(false)
const error = ref('')
const form = reactive({ phone: '', password: '', smsCode: '', newPassword: '' })
const codeText = ref('获取验证码')

function toggleMode() {
  mode.value = mode.value === 'login' ? 'init' : 'login'
  error.value = ''
}

function sendCode() {
  if (!/^1\d{10}$/.test(form.phone)) {
    uni.showToast({ title: '请先输入 11 位手机号', icon: 'none' })
    return
  }
  // 短信网关未接入前，后端 mock 模式接受任意 6 位验证码；这里只做倒计时与提示
  let left = 60
  codeText.value = left + 's'
  const t = setInterval(() => {
    left -= 1
    codeText.value = left + 's'
    if (left <= 0) {
      clearInterval(t)
      codeText.value = '获取验证码'
    }
  }, 1000)
  uni.showToast({ title: '验证码已发送（演示环境任意 6 位）', icon: 'none', duration: 2500 })
}

async function submit() {
  error.value = ''
  if (!/^1\d{10}$/.test(form.phone)) {
    error.value = '请输入正确的 11 位手机号'
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      if (!form.password) {
        error.value = '请输入密码'
        return
      }
      const data = await api.login(form.phone, form.password)
      setSession(data)
      uni.reLaunch({ url: '/pages/index/index' })
    } else {
      if (!form.smsCode || form.smsCode.length !== 6) {
        error.value = '请输入 6 位验证码'
        return
      }
      if (!form.newPassword || form.newPassword.length < 6) {
        error.value = '新密码至少 6 位'
        return
      }
      await api.initPassword(form.phone, form.smsCode, form.newPassword)
      const data = await api.login(form.phone, form.newPassword)
      setSession(data)
      uni.reLaunch({ url: '/pages/index/index' })
    }
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1a4fdc 0%, #2f6bff 42%, #f4f6fa 42%, #f4f6fa 100%);
  padding: 0 40rpx;
  box-sizing: border-box;
}

.hero {
  padding: 160rpx 10rpx 60rpx;
}

.brand {
  color: #ffffff;
  font-size: 56rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
}

.slogan {
  color: rgba(255, 255, 255, 0.8);
  font-size: 26rpx;
  margin-top: 16rpx;
}

.panel {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 44rpx 36rpx 36rpx;
  box-shadow: 0 16rpx 48rpx rgba(24, 48, 100, 0.12);
}

.panel-title {
  font-size: 38rpx;
  font-weight: 700;
  margin-bottom: 36rpx;
}

.field {
  margin-bottom: 30rpx;
}

.label {
  font-size: 26rpx;
  color: #6b7787;
  margin-bottom: 12rpx;
  display: block;
}

.input {
  height: 92rpx;
  background: #f6f8fc;
  border-radius: 14rpx;
  padding: 0 24rpx;
  font-size: 32rpx;
  box-sizing: border-box;
  width: 100%;
}

.ph {
  color: #b3bccb;
  font-size: 28rpx;
}

.flex1 {
  flex: 1;
}

.code-btn {
  margin-left: 16rpx;
  height: 92rpx;
  line-height: 92rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  border: 2rpx solid #2f6bff;
  color: #2f6bff;
  font-size: 26rpx;
}

.error {
  color: #d9363e;
  font-size: 26rpx;
  margin-bottom: 20rpx;
}

.loading {
  opacity: 0.7;
}

.switch {
  margin-top: 30rpx;
  text-align: center;
  color: #2f6bff;
  font-size: 26rpx;
}

.footer {
  text-align: center;
  padding: 48rpx 0 40rpx;
}
</style>
