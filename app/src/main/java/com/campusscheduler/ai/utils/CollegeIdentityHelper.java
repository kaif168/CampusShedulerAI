package com.campusscheduler.ai.utils;

import android.content.Context;
import android.widget.ImageView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.College;
import com.campusscheduler.ai.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CollegeIdentityHelper {

    public interface CollegeCallback {
        void onCollegeLoaded(College college);
    }

    private static final Map<String, College> CACHE = new HashMap<>();

    private static final Set<String> ACRONYMS = new HashSet<>(Arrays.asList(
            "hkbk", "nhce", "reva", "vtu", "aicte", "cse", "ise", "ece", "me", "cv", "iit", "iiit", "nit", "ugc", "nba", "naac"
    ));

    private static final Set<String> LOWERCASE_WORDS = new HashSet<>(Arrays.asList(
            "of", "and", "in", "for", "to", "at", "&"
    ));

    public static String formatCollegeName(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "HKBK College of Engineering";
        }
        String trimmed = raw.trim();
        String[] words = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            String lower = w.toLowerCase(Locale.ROOT);
            if (ACRONYMS.contains(lower)) {
                sb.append(lower.toUpperCase(Locale.ROOT));
            } else if (i > 0 && LOWERCASE_WORDS.contains(lower)) {
                sb.append(lower);
            } else if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase(Locale.ROOT));
                }
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static String formatVerifiedAddress(String rawAddress, String collegeName) {
        String cLower = (collegeName != null ? collegeName : "").toLowerCase(Locale.ROOT);
        String aLower = (rawAddress != null ? rawAddress : "").toLowerCase(Locale.ROOT);

        if (cLower.contains("hkbk") || aLower.contains("nagawara")) {
            return "Nagawara, Bengaluru, Karnataka 560045";
        }
        if (cLower.contains("nhce") || aLower.contains("marathalli") || aLower.contains("marathahalli")) {
            return "Marathahalli, Bengaluru, Karnataka 560103";
        }
        if (cLower.contains("reva") || aLower.contains("bagluru") || aLower.contains("bagalur")) {
            return "Bagalur, Bengaluru, Karnataka 562149";
        }
        if (rawAddress != null && !rawAddress.trim().isEmpty()) {
            String trimmed = rawAddress.trim();
            return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
        }
        return "Address Verification Required";
    }

    public static String getVerifiedCollegeAddress(String collegeId, String collegeName) {
        return formatVerifiedAddress(null, collegeName != null ? collegeName : collegeId);
    }

    public static int getCollegeLogoResId(String collegeId, String collegeName) {
        String cId = (collegeId != null ? collegeId : "").toLowerCase(Locale.ROOT);
        String cName = (collegeName != null ? collegeName : "").toLowerCase(Locale.ROOT);
        if (cId.contains("hkbk") || cName.contains("hkbk") || "college_1789406176044".equals(cId)) {
            return R.drawable.ic_hkbk_logo;
        }
        return R.drawable.ic_app_logo_main;
    }

    public static void bindCollegeLogo(String collegeId, String collegeName, ImageView imageView) {
        if (imageView == null) return;
        imageView.setImageResource(getCollegeLogoResId(collegeId, collegeName));
    }

    public static void resolveCollege(Context context, String collegeId, CollegeCallback callback) {
        if (collegeId == null || collegeId.trim().isEmpty()) {
            collegeId = LocalAuthManager.getSafeCollegeId(context, null);
        }

        final String finalCollegeId = collegeId;

        // 1. Check in-memory cache
        if (CACHE.containsKey(finalCollegeId)) {
            College cached = CACHE.get(finalCollegeId);
            if (callback != null) callback.onCollegeLoaded(cached);
            return;
        }

        // 2. Build preliminary from LocalAuthManager if available
        if (context != null) {
            LocalAuthManager authMgr = new LocalAuthManager(context);
            User localUser = authMgr.getCurrentUser();
            if (localUser != null && finalCollegeId.equals(localUser.getCollegeId())) {
                String localName = localUser.getCollegeName();
                String localAddr = localUser.getCollegeAddress();
                String localCode = localUser.getCollegeCode();
                if (localName != null && !localName.trim().isEmpty()) {
                    College preliminary = new College(
                            finalCollegeId,
                            formatCollegeName(localName),
                            localCode != null ? localCode : "CAMPUS",
                            formatVerifiedAddress(localAddr, localName),
                            "",
                            ""
                    );
                    CACHE.put(finalCollegeId, preliminary);
                    if (callback != null) callback.onCollegeLoaded(preliminary);
                }
            }
        }

        // 3. Query Cloud Firestore
        FirebaseHelper.getCollegesRef().document(finalCollegeId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        College college = doc.toObject(College.class);
                        if (college != null) {
                            if (college.getId() == null || college.getId().isEmpty()) {
                                college.setId(doc.getId());
                            }
                            college.setName(formatCollegeName(college.getName()));
                            college.setAddress(formatVerifiedAddress(college.getAddress(), college.getName()));
                            CACHE.put(finalCollegeId, college);

                            // Save to local session
                            if (context != null) {
                                new LocalAuthManager(context).updateCollegeIdentity(
                                        college.getId(),
                                        college.getName(),
                                        college.getAddress(),
                                        college.getCode()
                                );
                            }

                            if (callback != null) callback.onCollegeLoaded(college);
                            return;
                        }
                    }

                    // Fallback query by code if ID didn't match direct doc ID
                    FirebaseHelper.getCollegesRef().whereEqualTo("code", finalCollegeId.toLowerCase(Locale.ROOT)).limit(1).get()
                            .addOnSuccessListener(snap -> {
                                if (snap != null && !snap.isEmpty()) {
                                    DocumentSnapshot first = snap.getDocuments().get(0);
                                    College col = first.toObject(College.class);
                                    if (col != null) {
                                        col.setId(first.getId());
                                        col.setName(formatCollegeName(col.getName()));
                                        col.setAddress(formatVerifiedAddress(col.getAddress(), col.getName()));
                                        CACHE.put(finalCollegeId, col);
                                        if (context != null) {
                                            new LocalAuthManager(context).updateCollegeIdentity(
                                                    col.getId(), col.getName(), col.getAddress(), col.getCode()
                                            );
                                        }
                                        if (callback != null) callback.onCollegeLoaded(col);
                                        return;
                                    }
                                }

                                // Create safe default
                                College fallback = createSafeFallback(finalCollegeId);
                                CACHE.put(finalCollegeId, fallback);
                                if (callback != null) callback.onCollegeLoaded(fallback);
                            })
                            .addOnFailureListener(e -> {
                                College fallback = createSafeFallback(finalCollegeId);
                                CACHE.put(finalCollegeId, fallback);
                                if (callback != null) callback.onCollegeLoaded(fallback);
                            });
                })
                .addOnFailureListener(e -> {
                    College fallback = createSafeFallback(finalCollegeId);
                    CACHE.put(finalCollegeId, fallback);
                    if (callback != null) callback.onCollegeLoaded(fallback);
                });
    }

    private static College createSafeFallback(String collegeId) {
        String name = "HKBK College of Engineering";
        String code = "hkbkce";
        String addr = "Nagawara, Bengaluru, Karnataka 560045";
        if (collegeId != null && (collegeId.contains("NHCE") || collegeId.contains("1791270975963"))) {
            name = "New Horizon College of Engineering";
            code = "NHCE";
            addr = "Marathahalli, Bengaluru, Karnataka 560103";
        } else if (collegeId != null && (collegeId.contains("REVA") || collegeId.contains("1789717978002"))) {
            name = "REVA University";
            code = "rev-01";
            addr = "Bagalur, Bengaluru, Karnataka 562149";
        }
        return new College(collegeId, name, code, addr, "admin@campus.edu", "080-12345678");
    }
}
