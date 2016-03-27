package com.be.android.library.worker.demo.jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.be.android.library.worker.jobs.LoadJob;
import com.be.android.library.worker.models.LoadJobResult;
import com.be.library.worker.annotations.JobExtra;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class SimpleImageLoaderJob extends LoadJob {

    @JobExtra
    String mImageUrl;

    public SimpleImageLoaderJob() {
    }

    @Override
    protected void onPreExecute() throws Exception {
        super.onPreExecute();

        SimpleImageLoaderJobExtrasInjector.injectExtras(this);
    }

    @Override
    protected LoadJobResult<Bitmap> performLoad() throws Exception {
        final URL url = new URL(mImageUrl);

        // Synthetic delay
        Thread.sleep(4000);

        final InputStream mConn = url.openStream();
        try {
            final Bitmap result = BitmapFactory.decodeStream(mConn);

            if (result == null) {
                return LoadJobResult.loadFailure();
            }

            return new LoadJobResult<Bitmap>(result);

        } finally {
            try {
                mConn.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
