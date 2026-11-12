package com.lukachangtong.freight.web.controller;

import com.lukachangtong.freight.carrier.service.CarrierExpiryService;
import com.lukachangtong.freight.carrier.vo.ExpiryAlertVo;
import com.lukachangtong.freight.common.Result;
import com.lukachangtong.freight.infra.client.OcrClient;
import com.lukachangtong.freight.infra.client.dto.OcrDto.OcrResult;
import com.lukachangtong.freight.infra.entity.FileRecord;
import com.lukachangtong.freight.web.service.FileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 承运准入增强：证照 OCR 预填 + 证件到期预警。
 * OCR 结果只返回给前端预填表单，**不直接写准入档案**——落库仍走 /api/drivers、/api/vehicles 人工确认。
 */
@RestController
@RequestMapping("/api/carrier")
public class CarrierController {

    @Resource
    private OcrClient ocrClient;
    @Resource
    private CarrierExpiryService expiryService;
    @Resource
    private FileService fileService;

    @PostMapping("/ocr")
    public Result<OcrResult> ocr(@RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "ID_CARD") String type,
                                 @RequestParam(required = false) Long orgId) throws IOException {
        byte[] bytes = file.getBytes();
        OcrResult result = ocrClient.recognize(bytes, file.getOriginalFilename(), type);
        // 证照原件留档：审核与监管核查需要能回看原始图片
        if (result.isSuccess() && orgId != null && bytes.length > 0) {
            FileRecord record = fileService.upload(orgId, null, "OCR_" + type, null, file);
            result.setFileKey(record.getObsKey());
            result.setFileUrl(fileService.accessUrl(record.getObsKey()));
        }
        return Result.ok(result);
    }

    @GetMapping("/expiring")
    public Result<List<ExpiryAlertVo>> expiring(@RequestParam(required = false) Long orgId,
                                                @RequestParam(defaultValue = "60") int days) {
        return Result.ok(expiryService.listExpiring(orgId, days));
    }
}
