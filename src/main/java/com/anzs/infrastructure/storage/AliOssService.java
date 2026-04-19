package com.anzs.infrastructure.storage;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AliOssService {
    @Value("${oss.bucketName}")
    private String bucketName;

    private final OSS ossClient;

    public String generateUploadUrl(String objectName) {
        Date expiration = new Date(System.currentTimeMillis() + 5 * 60 * 1000); // 5分钟过期
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName, HttpMethod.PUT);
        request.setExpiration(expiration);
        URL url = ossClient.generatePresignedUrl(request);
        return url.toString();
    }


}
