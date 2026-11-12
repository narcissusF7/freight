package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.infra.entity.FileRecord;
import com.lukachangtong.freight.web.service.FileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 文件上传/访问。须鉴权（SecurityConfig 已配置 /api/files/** 需登录）。
 * TODO(M1): createBy 从 JWT 当前用户解析；orgId 当前由调用方传入，后续改为从令牌取。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    public Result<Long> upload(@RequestParam Long orgId,
                              @RequestParam String bizType,
                              @RequestParam(required = false) String bizId,
                              @RequestParam("file") MultipartFile file) throws IOException {
        FileRecord record = fileService.upload(orgId, null, bizType, bizId, file);
        return Result.ok(record.getId());
    }

    @GetMapping("/{id}/url")
    public Result<String> url(@PathVariable Long id) {
        // 返回对象存储 key 对应的访问地址；真实环境按权限签发临时直链，不长期暴露
        return Result.ok(fileService.accessUrl(String.valueOf(id)));
    }
}
