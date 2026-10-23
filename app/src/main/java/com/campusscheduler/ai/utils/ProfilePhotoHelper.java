package com.campusscheduler.ai.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import com.campusscheduler.ai.models.User;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfilePhotoHelper {

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> memoryCache;

    static {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public interface OnPhotoUploadedCallback {
        void onSuccess(String photoUrl);
        void onFailure(Exception e);
    }

    public static void showPhotoOptionsDialog(Activity activity, User currentUser, ActivityResultLauncher<Intent> launcher, Runnable onPhotoRemoved) {
        String[] options = (currentUser != null && currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty())
                ? new String[]{"Choose New Photo from Gallery", "Remove Profile Photo"}
                : new String[]{"Choose Photo from Gallery"};

        new AlertDialog.Builder(activity)
                .setTitle("Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        launcher.launch(Intent.createChooser(intent, "Select Profile Photo"));
                    } else if (which == 1) {
                        removeProfilePhoto(activity, currentUser, onPhotoRemoved);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void uploadProfilePhoto(Activity activity, Uri imageUri, User currentUser, OnPhotoUploadedCallback callback) {
        if (currentUser == null) return;
        String uid = currentUser.getUid() != null ? currentUser.getUid() :
                (currentUser.getEmail() != null ? currentUser.getEmail().replaceAll("[^a-zA-Z0-9]", "_") : "user");
        String collegeId = currentUser.getCollegeId() != null ? currentUser.getCollegeId() : "COLLEGE_1789406176044";

        ProgressDialog pd = new ProgressDialog(activity);
        pd.setTitle("Updating Profile Photo");
        pd.setMessage("Uploading high-resolution avatar to secure storage...");
        pd.setCancelable(false);
        pd.show();

        String filename = uid + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseHelper.getStorageRef()
                .child("profile_photos")
                .child(collegeId)
                .child(filename);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                pd.dismiss();
                                String photoUrl = downloadUri.toString();
                                applyPhotoUrlToUser(activity, currentUser, photoUrl);
                                Toast.makeText(activity, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show();
                                if (callback != null) callback.onSuccess(photoUrl);
                            })
                            .addOnFailureListener(e -> {
                                pd.dismiss();
                                Toast.makeText(activity, "Failed to retrieve photo URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                if (callback != null) callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(activity, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onFailure(e);
                });
    }

    private static void applyPhotoUrlToUser(Activity activity, User currentUser, String photoUrl) {
        currentUser.setPhotoUrl(photoUrl);
        LocalAuthManager.saveUserSession(activity, currentUser);

        String uid = currentUser.getUid();
        if (uid != null) {
            FirebaseHelper.getUsersRef().document(uid).update("photoUrl", photoUrl);
        }

        String email = currentUser.getEmail();
        if (User.ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole()) || "Faculty".equalsIgnoreCase(currentUser.getRole())) {
            if (email != null) {
                FirebaseHelper.getFacultyRef().whereEqualTo("email", email).get()
                        .addOnSuccessListener(snap -> {
                            if (snap != null && !snap.isEmpty()) {
                                snap.getDocuments().get(0).getReference().update("photoUrl", photoUrl);
                            }
                        });
            }
        } else if (User.ROLE_STUDENT.equalsIgnoreCase(currentUser.getRole())) {
            if (email != null) {
                FirebaseHelper.getStudentsRef().whereEqualTo("email", email).get()
                        .addOnSuccessListener(snap -> {
                            if (snap != null && !snap.isEmpty()) {
                                snap.getDocuments().get(0).getReference().update("photoUrl", photoUrl);
                            }
                        });
            }
        }
    }

    private static void removeProfilePhoto(Activity activity, User currentUser, Runnable onPhotoRemoved) {
        if (currentUser == null) return;
        applyPhotoUrlToUser(activity, currentUser, null);
        Toast.makeText(activity, "Profile photo removed.", Toast.LENGTH_SHORT).show();
        if (onPhotoRemoved != null) onPhotoRemoved.run();
    }

    public static void bindProfileAvatar(Activity activity, String photoUrl, ImageView... imageViews) {
        if (imageViews == null || imageViews.length == 0) return;

        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            for (ImageView iv : imageViews) {
                if (iv != null) {
                    iv.setImageResource(android.R.drawable.ic_menu_myplaces);
                }
            }
            return;
        }

        Bitmap cached = memoryCache.get(photoUrl);
        if (cached != null && !cached.isRecycled()) {
            for (ImageView iv : imageViews) {
                if (iv != null) iv.setImageBitmap(cached);
            }
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL(photoUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.setDoInput(true);
                conn.connect();

                InputStream is = conn.getInputStream();
                Bitmap raw = BitmapFactory.decodeStream(is);
                is.close();
                conn.disconnect();

                if (raw != null) {
                    Bitmap circular = getCircularBitmap(raw);
                    memoryCache.put(photoUrl, circular);

                    activity.runOnUiThread(() -> {
                        for (ImageView iv : imageViews) {
                            if (iv != null) {
                                iv.setImageBitmap(circular);
                            }
                        }
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int minEdge = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(minEdge, minEdge, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        float radius = minEdge / 2.0f;
        canvas.drawCircle(radius, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        int left = (width - minEdge) / 2;
        int top = (height - minEdge) / 2;
        Rect srcRect = new Rect(left, top, left + minEdge, top + minEdge);
        Rect dstRect = new Rect(0, 0, minEdge, minEdge);
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);

        return output;
    }
}
