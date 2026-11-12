package com.lukachangtong.freight.web.service;

import com.lukachangtong.freight.infra.client.FileClient;
import com.lukachangtong.freight.infra.client.FileUploadResult;
import com.lukachangtong.freight.infra.entity.FileRecord;
import com.lukachangtong.freight.infra.mapper.FileRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;

/** 文件上传业务：委托 FileClient 存储，索引落 file_record。 */
@Service
public class FileService {

    @Resource
    private FileClient fileClient;
    @Resource
    private FileRecordMapper fileRecordMapper;

    public FileRecord upload(Long orgId, Long createBy, String bizType, String bizId, MultipartFile file)
            throws IOException {
        FileUploadResult result = fileClient.upload(
                file.getInputStream(), file.getOriginalFilename(), file.getContentType());
        FileRecord record = new FileRecord();
        record.setOrgId(orgId);
        record.setCreateBy(createBy);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setObsKey(result.getFileKey());
        record.setFileName(result.getFileName());
        record.setContentType(result.getContentType());
        record.setSizeBytes(result.getSizeBytes());
        fileRecordMapper.insert(record);
        return record;
    }

    public String accessUrl(String fileKey) {
        return fileClient.getAccessUrl(fileKey);
    }
}
