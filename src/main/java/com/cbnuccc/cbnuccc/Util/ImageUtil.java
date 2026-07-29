package com.cbnuccc.cbnuccc.Util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import net.coobird.thumbnailator.Thumbnails;

public class ImageUtil {
    public static DataWithStatusCode<MultipartFile> makeImageLowQuality(MultipartFile file) {
        // 이미지 데이터 가져오기
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            LogUtil.printBasicWarnLog(LogHeader.COMPRESS_IMAGE, LogUtil.makeExceptionKV(e));
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            // 이미지 압축하기
            Thumbnails.of(originalImage)
                    .size(originalImage.getWidth(), originalImage.getHeight())
                    .outputFormat("jpg")
                    .outputQuality(0.7)
                    .toOutputStream(os);
        } catch (IOException e) {
            LogUtil.printBasicWarnLog(LogHeader.COMPRESS_IMAGE, LogUtil.makeExceptionKV(e));
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }

        // 반환하기
        byte[] compressedImage = os.toByteArray();
        MultipartFile result = new MockMultipartFile("file", "compressed.jpg", "image/jpg", compressedImage);
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, result);
    }
}
