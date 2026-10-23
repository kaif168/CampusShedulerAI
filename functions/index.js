const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * provisionTeacherAccount
 *
 * Callable Cloud Function to provision a Teacher account via Firebase Admin SDK:
 * 1. Verifies caller is authenticated and holds ROLE_COLLEGE_ADMIN or ROLE_OFFICIAL_OWNER.
 * 2. Enforces college tenant isolation (caller's collegeId matches teacher's collegeId).
 * 3. Supports all syntactically valid emails (Gmail, Outlook, .edu.in, .ac.in, etc.).
 * 4. Creates user in Firebase Auth if not already existing, without signing out the caller.
 * 5. Generates the secure password setup link and dispatches/returns it.
 * 6. Synchronizes Firestore users/{authUid} and faculty/{facultyId} profiles.
 */
exports.provisionTeacherAccount = functions.https.onCall(async (data, context) => {
  // 1. Authenticate Caller
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The operation must be called while authenticated as a College Admin or Official Owner."
    );
  }

  const callerUid = context.auth.uid;

  // 2. Fetch and Authorize Caller Role from Firestore
  const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
  if (!callerDoc.exists) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Caller profile does not exist in Cloud Firestore."
    );
  }

  const callerData = callerDoc.data();
  const callerRole = callerData.role;
  const isOfficialOwner = callerRole === "OFFICIAL_OWNER";
  const isCollegeAdmin = callerRole === "COLLEGE_ADMIN" || callerRole === "ADMIN";

  if (!isOfficialOwner && !isCollegeAdmin) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only College Administrators and Official Owners have permission to provision faculty members."
    );
  }

  // 3. Validate Input Data
  const { facultyId, name, email, designation, collegeId, departmentId, phone } = data;

  if (!email || typeof email !== "string" || !email.includes("@")) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "A valid email address (personal or institutional) is required."
    );
  }

  const cleanEmail = email.trim().toLowerCase();
  const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
  if (!emailRegex.test(cleanEmail)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The email address format is not syntactically valid."
    );
  }

  // 4. College Tenant Isolation Check
  const targetCollegeId = collegeId || callerData.collegeId;
  if (!isOfficialOwner && callerData.collegeId && callerData.collegeId !== targetCollegeId) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "You are not authorized to provision teachers for another college."
    );
  }

  let userRecord;
  let isNewAccount = false;

  // 5. Check if user already exists in Firebase Auth
  try {
    userRecord = await admin.auth().getUserByEmail(cleanEmail);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      // Create user in Firebase Authentication
      userRecord = await admin.auth().createUser({
        email: cleanEmail,
        displayName: name || "Faculty Member",
        emailVerified: false,
        disabled: false
      });
      isNewAccount = true;
    } else {
      throw new functions.https.HttpsError("internal", error.message);
    }
  }

  const authUid = userRecord.uid;

  // 6. Generate Password Reset / Setup Link
  let actionLink = "";
  try {
    actionLink = await admin.auth().generatePasswordResetLink(cleanEmail);
  } catch (err) {
    console.warn("Could not generate password reset link directly:", err.message);
  }

  // 7. Synchronize Firestore Profile in users/{authUid}
  const teacherUserData = {
    uid: authUid,
    name: name || userRecord.displayName || "Faculty Member",
    email: cleanEmail,
    role: "TEACHER",
    collegeId: targetCollegeId,
    departmentId: departmentId || "DEPT_CSE",
    phone: phone || "",
    designation: designation || "Faculty",
    active: true,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await admin.firestore().collection("users").doc(authUid).set(teacherUserData, { merge: true });

  // 8. Update faculty record if facultyId provided
  if (facultyId) {
    await admin.firestore().collection("faculty").doc(facultyId).update({
      uid: authUid,
      email: cleanEmail,
      authProvisioned: true,
      provisionStatus: "PROVISIONED",
      provisionedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  }

  return {
    success: true,
    isNewAccount,
    authUid,
    email: cleanEmail,
    teacherName: name,
    message: isNewAccount
      ? "Teacher account created and linked. Password setup link generated."
      : "Teacher account already registered in Firebase Auth. Profile linked and setup link refreshed.",
    actionLink
  };
});
