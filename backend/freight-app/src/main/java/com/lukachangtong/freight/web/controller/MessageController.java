package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.message.dto.MessageNotifyRequest;
import com.lukachangtong.freight.message.dto.TemplateSaveRequest;
import com.lukachangtong.freight.message.entity.MsgRecord;
import com.lukachangtong.freight.message.entity.MsgTemplate;
import com.lukachangtong.freight.message.service.MessageService;
import com.lukachangtong.freight.message.service.MessageTemplateService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 消息中心：四通道记录、站内信收件箱、模板配置、手动投递/重发。
 *
 * 投递默认是异步的（落 PENDING 由调度器发），这里提供 /dispatch 与 /{id}/resend
 * 便于演示与排障——不打开后台任务也能把消息发出去。
 */
@RestController
@RequestMapping("/api")
public class MessageController {

    @Resource
    private MessageService messageService;
    @Resource
    private MessageTemplateService templateService;

    @GetMapping("/messages")
    public Result<List<MsgRecord>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizKey,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(messageService.list(status, channel, bizType, bizKey, limit));
    }

    @GetMapping("/messages/inbox")
    public Result<List<MsgRecord>> inbox(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(messageService.inbox(orgId, unread, limit));
    }

    @PostMapping("/messages/inbox/read")
    public Result<Integer> markRead(@RequestBody Map<String, List<Long>> body) {
        return Result.ok(messageService.markInboxRead(body.get("ids")));
    }

    /** 手动投递到期消息（等价于调度器跑一轮）。 */
    @PostMapping("/messages/dispatch")
    public Result<Integer> dispatch(@RequestParam(required = false) Integer limit) {
        return Result.ok(messageService.dispatchDue(limit == null ? 200 : limit));
    }

    @PostMapping("/messages/{id}/resend")
    public Result<MsgRecord> resend(@PathVariable Long id) {
        return Result.ok(messageService.resend(id));
    }

    /** 手工发消息（演示/补发）：按业务节点模板群发到对应通道。 */
    @PostMapping("/messages/notify")
    public Result<Integer> notify(@RequestBody MessageNotifyRequest request) {
        return Result.ok(messageService.notify(request.getOrgId(), request.getBizType(), request.getBizKey(),
                request.getVars(), request.getReceivers()));
    }

    // ------------------------------------------------------------------ 模板（节点配置化）

    @GetMapping("/message-templates")
    public Result<List<MsgTemplate>> listTemplates(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String channel) {
        return Result.ok(templateService.list(orgId, bizType, channel));
    }

    @PostMapping("/message-templates")
    public Result<Long> createTemplate(@RequestBody TemplateSaveRequest request) {
        return Result.ok(templateService.create(request));
    }

    @PutMapping("/message-templates/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody TemplateSaveRequest request) {
        templateService.update(id, request);
        return Result.ok(null);
    }

    @DeleteMapping("/message-templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateService.delete(id);
        return Result.ok(null);
    }
}
