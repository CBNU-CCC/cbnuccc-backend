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
        // get image data
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_MISSION_IMAGE, e.getMessage(), null);
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            // compress image.
            Thumbnails.of(originalImage)
                    .size(originalImage.getWidth(), originalImage.getHeight())
                    .outputFormat("jpg")
                    .outputQuality(0.65)
                    .toOutputStream(os);
        } catch (IOException e) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_MISSION_IMAGE, e.getMessage(), null);
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }

        // return it
        byte[] compressedImage = os.toByteArray();
        MultipartFile result = new MockMultipartFile("file", "compressed.jpg", "image/jpg", compressedImage);
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, result);
    }
}
