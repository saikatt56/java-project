package com.example.digitalwellbeing.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.digitalwellbeing.firebase.FirebaseUploader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirebaseSyncWorker extends Worker {

    public FirebaseSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        FirebaseUploader.uploadAllData(getApplicationContext(), new FirebaseUploader.UploadCallback() {
            @Override
            public void onSuccess() {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                success.set(false);
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            if (!completed) {
                return Result.retry();
            }
        } catch (InterruptedException e) {
            return Result.retry();
        }

        return success.get() ? Result.success() : Result.retry();
    }
}
