package com.lukachangtong.freight.api.finance;

/**
 * 支付成功后的入账端口，由结算域实现。
 *
 * 调用约定：
 * 1. **必须在支付单更新为 SUCCESS 的同一个事务内调用**——钱出去了但没记账，
 *    或者记了账但支付单没成功，都会让日终对账永远平不了；
 * 2. 幂等：同一 `bizType + bizNo` 只入账一次，重复调用返回首次结果；
 * 3. 抛异常必须让支付单事务回滚，不能吞。
 */
public interface PaymentPostingPort {

    PostingResult post(PaymentPostingCommand command);

    class PostingResult {
        private boolean success;
        private String flowNo;
        /** 入账后钱包余额，便于前端/日志核对 */
        private java.math.BigDecimal balanceAfter;
        private String message;

        public static PostingResult ok(String flowNo, java.math.BigDecimal balanceAfter) {
            PostingResult r = new PostingResult();
            r.success = true;
            r.flowNo = flowNo;
            r.balanceAfter = balanceAfter;
            r.message = "posted";
            return r;
        }

        public static PostingResult fail(String message) {
            PostingResult r = new PostingResult();
            r.success = false;
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public String getFlowNo() { return flowNo; }
        public java.math.BigDecimal getBalanceAfter() { return balanceAfter; }
        public String getMessage() { return message; }
    }
}
