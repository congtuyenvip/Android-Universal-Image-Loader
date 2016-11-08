package com.ctvip.uil.core.decode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ctvip.uil.core.ImageLoaderConfiguration;
import com.ctvip.uil.core.pool.BitmapPoolFactory;
import com.ctvip.uil.utils.IoUtils;
import com.ctvip.uil.utils.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Nguyen Cong Tuyen on 11/8/2016.
 */

public class ImagePoolDecoder extends BaseImageDecoder {
    /**
     * @param loggingEnabled Whether debug logs will be written to LogCat. Usually should match {@link
     *                       ImageLoaderConfiguration.Builder#writeDebugLogs()
     *                       ImageLoaderConfiguration.writeDebugLogs()}
     */
    public ImagePoolDecoder(boolean loggingEnabled) {
        super(loggingEnabled);
    }

    @Override
    public Bitmap decode(ImageDecodingInfo decodingInfo) throws IOException {
        if (decodingInfo != null && decodingInfo.getDecodingOptions() != null && decodingInfo.getDecodingOptions().inMutable == false) {
            return super.decode(decodingInfo);
        }
        Bitmap decodedBitmap;
        ImageFileInfo imageInfo;

        InputStream imageStream = getImageStream(decodingInfo);
        if (imageStream == null) {
            Logger.e(ERROR_NO_IMAGE_STREAM, decodingInfo.getImageKey());
            return null;
        }
        try {
            imageInfo = defineImageSizeAndRotation(imageStream, decodingInfo);
            imageStream = resetStream(imageStream, decodingInfo);
            BitmapFactory.Options decodingOptions = prepareDecodingOptions(imageInfo.imageSize, decodingInfo);
            decodingOptions.outHeight = imageInfo.imageSize.getHeight();
            decodingOptions.outWidth = imageInfo.imageSize.getWidth();
            decodedBitmap = BitmapPoolFactory.decodeStream(imageStream, decodingOptions);
        } finally {
            IoUtils.closeSilently(imageStream);
        }

        if (decodedBitmap == null) {
            Logger.e(ERROR_CANT_DECODE_IMAGE, decodingInfo.getImageKey());
        }
        else {
            decodedBitmap = considerExactScaleAndOrientatiton(decodedBitmap, decodingInfo, imageInfo.exif.rotation,
                    imageInfo.exif.flipHorizontal);
        }
        return decodedBitmap;
    }
}
