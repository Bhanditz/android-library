/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.common.network;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * A Request body that represents a file and include information about the progress when uploading it
 *
 * @author David González Verdugo
 */
public class FileRequestBody extends RequestBody implements ProgressiveDataTransferer {

    private File mFile;
    private MediaType mContentType;
    Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<>();

    public FileRequestBody(File file, MediaType contentType) {
        mFile = file;
        mContentType = contentType;
    }

    @Override
    public MediaType contentType() {
        return mContentType;
    }

    @Override
    public void writeTo(BufferedSink sink) {
        Source source;
        Iterator<OnDatatransferProgressListener> it;
        try {
            source = Okio.source(mFile);
            long transferred = 0;
            long read;

            while ((read = source.read(sink.buffer(), 2048)) != -1) {
                transferred += read;
                sink.flush();
                synchronized (mDataTransferListeners) {
                    it = mDataTransferListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onTransferProgress(read, transferred, mFile.length(), mFile.getAbsolutePath());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
    }

    @Override
    public void addDatatransferProgressListeners(Collection<OnDatatransferProgressListener> listeners) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.addAll(listeners);
        }
    }

    @Override
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
    }
}