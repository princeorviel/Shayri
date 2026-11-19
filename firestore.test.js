const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_poet_1";
const BOB_UID = "bob_poet_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validShayariPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    penName: "अमन मिर्ज़ा",
    couplet: "चाँदनी रात में हवाओं ने पैग़ाम दिया है,\nहर बूँद ने आज ज़मीं को नया नाम दिया है।",
    categoryId: "prakriti",
    promptTitle: "बारिश और यादें",
    status: "published",
    wahCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list or create community_shayari", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("community_shayari").where("status", "==", "published").get());
  await assertFails(unauthDb.collection("community_shayari").doc("post1").set(validShayariPayload(ALICE_UID)));
});

test("Authenticated user can publish valid community_shayari and list published posts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("community_shayari").doc("post_1").set(validShayariPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("community_shayari").where("status", "==", "published").get()
  );
});

test("Authenticated user cannot list community_shayari without status == published filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("community_shayari").get());
});

test("Identity spoofing and ghost fields are rejected on create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("community_shayari").doc("spoof_1").set(validShayariPayload(BOB_UID))
  );
  await assertFails(
    aliceDb.collection("community_shayari").doc("ghost_1").set(validShayariPayload(ALICE_UID, { isVerified: true }))
  );
  await assertFails(
    aliceDb.collection("community_shayari").doc("wah_cheat").set(validShayariPayload(ALICE_UID, { wahCount: 99 }))
  );
});

test("Short couplet or invalid categoryId is rejected, while all new categories are accepted", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("community_shayari").doc("short_1").set(validShayariPayload(ALICE_UID, { couplet: "छोटा" }))
  );
  await assertFails(
    aliceDb.collection("community_shayari").doc("bad_cat").set(validShayariPayload(ALICE_UID, { categoryId: "unknown_cat" }))
  );
  const newCategories = [
    "bewafai",
    "sharabi",
    "khuda_rab",
    "maut_janaza",
    "secret_love",
    "romantic",
    "gen_z",
    "nibba_nibbi",
  ];
  for (const catId of newCategories) {
    await assertSucceeds(
      aliceDb
        .collection("community_shayari")
        .doc(`cat_${catId}`)
        .set(validShayariPayload(ALICE_UID, { categoryId: catId }))
    );
  }
});

test("Cross-user edit and delete are rejected, owner edit and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("community_shayari").doc("post_edit").set(validShayariPayload(ALICE_UID))
  );

  await assertFails(
    bobDb.collection("community_shayari").doc("post_edit").update({
      couplet: "किसी और की शायरी बदलने की कोशिश जो अस्वीकार्य है।",
      updatedAt: new Date(),
    })
  );
  await assertFails(bobDb.collection("community_shayari").doc("post_edit").delete());

  await assertSucceeds(
    aliceDb.collection("community_shayari").doc("post_edit").update({
      couplet: "नया मौसम और नई उम्मीदें साथ लाए हैं,\nदिल के बागीचे में फिर फूल खिल आए हैं।",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("community_shayari").doc("post_edit").delete());
});

test("Atomic Wah increment and decrement succeed only via batch with user wah marker", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("community_shayari").doc("post_wah").set(validShayariPayload(ALICE_UID))
  );

  // Non-atomic direct increment without wah marker must fail
  await assertFails(
    bobDb.collection("community_shayari").doc("post_wah").update({ wahCount: 1 })
  );

  // Inflated increment (+5) in batch must fail
  const badBatch = bobDb.batch();
  badBatch.set(bobDb.collection("users").doc(BOB_UID).collection("wahs").doc("post_wah"), {
    shayariId: "post_wah",
    createdAt: new Date(),
  });
  badBatch.update(bobDb.collection("community_shayari").doc("post_wah"), { wahCount: 5 });
  await assertFails(badBatch.commit());

  // Valid atomic +1 batch succeeds
  const addBatch = bobDb.batch();
  addBatch.set(bobDb.collection("users").doc(BOB_UID).collection("wahs").doc("post_wah"), {
    shayariId: "post_wah",
    createdAt: new Date(),
  });
  addBatch.update(bobDb.collection("community_shayari").doc("post_wah"), { wahCount: 1 });
  await assertSucceeds(addBatch.commit());

  // Valid atomic -1 batch succeeds
  const removeBatch = bobDb.batch();
  removeBatch.delete(bobDb.collection("users").doc(BOB_UID).collection("wahs").doc("post_wah"));
  removeBatch.update(bobDb.collection("community_shayari").doc("post_wah"), { wahCount: 0 });
  await assertSucceeds(removeBatch.commit());
});

test("Private user profile is isolated per owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      penName: "सहर",
      bio: "दिल की बातें शब्दों में",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wahs").get());
});

test("Personal shayari collection allows owner CRUD and blocks unauthenticated or cross-user access", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const aliceCol = aliceDb.collection("users").doc(ALICE_UID).collection("personal_shayari");

  // Unauthenticated caller denied
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("personal_shayari").get()
  );

  // Reject missing both title and themeTag
  await assertFails(
    aliceCol.doc("missing_label").set({
      userId: ALICE_UID,
      title: "",
      themeTag: "",
      couplet: "चाँदनी रात में हवाओं ने आज फिर गीत गाया है।",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject ghost field or spoofed userId
  await assertFails(
    aliceCol.doc("ghost_personal").set({
      userId: ALICE_UID,
      title: "बरसात",
      themeTag: "प्रकृति",
      couplet: "चाँदनी रात में हवाओं ने आज फिर गीत गाया है।",
      extra: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceCol.doc("spoof_personal").set({
      userId: BOB_UID,
      title: "बरसात",
      themeTag: "प्रकृति",
      couplet: "चाँदनी रात में हवाओं ने आज फिर गीत गाया है।",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid create succeeds (title + themeTag)
  await assertSucceeds(
    aliceCol.doc("diary_1").set({
      userId: ALICE_UID,
      title: "सुकून की शाम",
      themeTag: "वक़्त व सुकून",
      couplet: "शाम ढली तो मन ने ख़ामोशी से बात की,\nहर उलझन से परे एक मीठी शुरुआत की।",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner list & update succeed
  await assertSucceeds(aliceCol.get());
  await assertSucceeds(
    aliceCol.doc("diary_1").update({
      title: "शाम का सुकून",
      themeTag: "सुकून",
      updatedAt: new Date(),
    })
  );

  // Cross-user read/list/write/delete denied
  const bobViewAlice = bobDb.collection("users").doc(ALICE_UID).collection("personal_shayari");
  await assertFails(bobViewAlice.get());
  await assertFails(bobViewAlice.doc("diary_1").get());
  await assertFails(
    bobViewAlice.doc("diary_1").update({ title: "छल", updatedAt: new Date() })
  );
  await assertFails(bobViewAlice.doc("diary_1").delete());

  // Owner delete succeeds
  await assertSucceeds(aliceCol.doc("diary_1").delete());
});

test("Shayari comments enforce auth, schema bounds, and owner-only delete", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const validComment = (uid, overrides = {}) => ({
    shayariKey: "seed_1",
    userId: uid,
    authorName: "अमन",
    text: "बहुत खूबसूरत शायरी!",
    status: "published",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  });

  // Unauthenticated read/write denied
  await assertFails(
    unauthDb
      .collection("shayari_comments")
      .where("status", "==", "published")
      .where("shayariKey", "==", "seed_1")
      .get()
  );
  await assertFails(
    unauthDb.collection("shayari_comments").doc("c_unauth").set(validComment(ALICE_UID))
  );

  // Spoofed userId, ghost field, empty text, or missing status filter rejected
  await assertFails(
    aliceDb.collection("shayari_comments").doc("c_spoof").set(validComment(BOB_UID))
  );
  await assertFails(
    aliceDb
      .collection("shayari_comments")
      .doc("c_ghost")
      .set(validComment(ALICE_UID, { admin: true }))
  );
  await assertFails(
    aliceDb
      .collection("shayari_comments")
      .doc("c_empty")
      .set(validComment(ALICE_UID, { text: "" }))
  );
  await assertFails(aliceDb.collection("shayari_comments").get());

  // Valid create & filtered list succeed
  await assertSucceeds(
    aliceDb.collection("shayari_comments").doc("c_1").set(validComment(ALICE_UID))
  );
  await assertSucceeds(
    bobDb
      .collection("shayari_comments")
      .where("status", "==", "published")
      .where("shayariKey", "==", "seed_1")
      .get()
  );

  // Cross-user update & delete rejected
  await assertFails(
    bobDb.collection("shayari_comments").doc("c_1").update({
      text: "बदला हुआ कमेंट",
      updatedAt: new Date(),
    })
  );
  await assertFails(bobDb.collection("shayari_comments").doc("c_1").delete());

  // Owner update & delete succeed
  await assertSucceeds(
    aliceDb.collection("shayari_comments").doc("c_1").update({
      text: "वाह, लाजवाब!",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("shayari_comments").doc("c_1").delete());
});

test("Post activity notifications enforce recipient privacy, valid post ownership, and no spoofing", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("community_shayari").doc("post_notif_1").set(validShayariPayload(ALICE_UID))
  );

  const validNotif = (type, overrides = {}) => ({
    recipientUserId: ALICE_UID,
    actorUserId: BOB_UID,
    actorName: "बॉब शायर",
    type,
    postId: "post_notif_1",
    postExcerpt: "चाँदनी रात में हवाओं ने पैग़ाम दिया है",
    commentText: type === "comment" ? "बहुत खूब लिखा है!" : "",
    isRead: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  });

  const aliceInboxFromBob = bobDb.collection("users").doc(ALICE_UID).collection("notifications");
  const aliceInboxOwn = aliceDb.collection("users").doc(ALICE_UID).collection("notifications");

  // Unauthenticated caller denied
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notifications").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_unauth").set(validNotif("like"))
  );

  // Self-notification denied
  await assertFails(
    aliceInboxOwn.doc("n_self").set(validNotif("like", { actorUserId: ALICE_UID }))
  );

  // Spoofed actor, already-read create, non-existent post, invalid type, or comment without text denied
  await assertFails(
    aliceInboxFromBob.doc("n_spoof").set(validNotif("like", { actorUserId: "charlie_99" }))
  );
  await assertFails(
    aliceInboxFromBob.doc("n_read").set(validNotif("like", { isRead: true }))
  );
  await assertFails(
    aliceInboxFromBob.doc("n_missing_post").set(validNotif("like", { postId: "non_existent" }))
  );
  await assertFails(
    aliceInboxFromBob.doc("n_bad_type").set(validNotif("repost"))
  );
  await assertFails(
    aliceInboxFromBob.doc("n_empty_comment").set(validNotif("comment", { commentText: "" }))
  );

  // Bob can notify Alice on like, comment, and share
  await assertSucceeds(aliceInboxFromBob.doc("n_like").set(validNotif("like")));
  await assertSucceeds(aliceInboxFromBob.doc("n_comment").set(validNotif("comment")));
  await assertSucceeds(aliceInboxFromBob.doc("n_share").set(validNotif("share")));

  // Bob cannot list, update, or delete Alice's inbox
  await assertFails(aliceInboxFromBob.get());
  await assertFails(aliceInboxFromBob.doc("n_like").get());
  await assertFails(aliceInboxFromBob.doc("n_like").update({ isRead: true, updatedAt: new Date() }));
  await assertFails(aliceInboxFromBob.doc("n_like").delete());

  // Alice can list, mark as read, and delete her notifications, but cannot tamper with immutable fields
  await assertSucceeds(aliceInboxOwn.get());
  await assertFails(
    aliceInboxOwn.doc("n_like").update({
      isRead: true,
      actorName: "बदला नाम",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceInboxOwn.doc("n_like").update({ isRead: true, updatedAt: new Date() })
  );
  await assertSucceeds(aliceInboxOwn.doc("n_like").delete());
});

test("Public poet profiles enforce owner-only edits, scoped listing, and atomic follow/unfollow", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const validPoetProfile = (uid, penName, overrides = {}) => ({
    userId: uid,
    penName,
    bio: "शब्दों से दिल तक का सफ़र",
    isPublic: true,
    followerCount: 0,
    followingCount: 0,
    postCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  });

  // Unauthenticated access denied
  await assertFails(
    unauthDb.collection("poet_profiles").where("isPublic", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("poet_profiles").doc(ALICE_UID).set(validPoetProfile(ALICE_UID, "अमन मिर्ज़ा"))
  );

  // Inflated followerCount or spoofed userId or non-public profile rejected
  await assertFails(
    aliceDb.collection("poet_profiles").doc(ALICE_UID).set(validPoetProfile(ALICE_UID, "अमन मिर्ज़ा", { followerCount: 500 }))
  );
  await assertFails(
    aliceDb.collection("poet_profiles").doc(BOB_UID).set(validPoetProfile(BOB_UID, "बॉब"))
  );
  await assertFails(
    aliceDb.collection("poet_profiles").doc(ALICE_UID).set(validPoetProfile(ALICE_UID, "अमन मिर्ज़ा", { isPublic: false }))
  );

  // Valid creates succeed
  await assertSucceeds(
    aliceDb.collection("poet_profiles").doc(ALICE_UID).set(validPoetProfile(ALICE_UID, "अमन मिर्ज़ा"))
  );
  await assertSucceeds(
    bobDb.collection("poet_profiles").doc(BOB_UID).set(validPoetProfile(BOB_UID, "सहर"))
  );

  // Unscoped list fails; scoped list succeeds
  await assertFails(bobDb.collection("poet_profiles").get());
  await assertSucceeds(bobDb.collection("poet_profiles").where("isPublic", "==", true).get());

  // Cross-user bio edit and owner followerCount inflation rejected
  await assertFails(
    bobDb.collection("poet_profiles").doc(ALICE_UID).update({ bio: "हैक", updatedAt: new Date() })
  );
  await assertFails(
    aliceDb.collection("poet_profiles").doc(ALICE_UID).update({ followerCount: 100, updatedAt: new Date() })
  );

  // Owner bio update succeeds
  await assertSucceeds(
    aliceDb.collection("poet_profiles").doc(ALICE_UID).update({
      penName: "अमन मिर्ज़ा 'सहर'",
      bio: "दिल की हर बात शायरी में",
      postCount: 2,
      updatedAt: new Date(),
    })
  );

  // Self-follow and non-atomic followerCount update rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("following").doc(ALICE_UID).set({
      targetUserId: ALICE_UID,
      targetPenName: "अमन मिर्ज़ा 'सहर'",
      createdAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("poet_profiles").doc(ALICE_UID).update({ followerCount: 1 })
  );

  // Atomic follow batch succeeds (Bob follows Alice)
  const followBatch = bobDb.batch();
  followBatch.set(bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID), {
    targetUserId: ALICE_UID,
    targetPenName: "अमन मिर्ज़ा 'सहर'",
    createdAt: new Date(),
  });
  followBatch.set(bobDb.collection("poet_profiles").doc(ALICE_UID).collection("followers").doc(BOB_UID), {
    followerUserId: BOB_UID,
    followerName: "सहर",
    createdAt: new Date(),
  });
  followBatch.update(bobDb.collection("poet_profiles").doc(ALICE_UID), { followerCount: 1 });
  followBatch.update(bobDb.collection("poet_profiles").doc(BOB_UID), {
    followingCount: 1,
    updatedAt: new Date(),
  });
  await assertSucceeds(followBatch.commit());

  // Alice cannot inspect Bob's private following list; Alice can list her own followers
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("following").get());
  await assertSucceeds(aliceDb.collection("poet_profiles").doc(ALICE_UID).collection("followers").get());

  // Follow notification succeeds with postId == 'profile', rejects invalid postId
  const bobToAliceNotif = bobDb.collection("users").doc(ALICE_UID).collection("notifications");
  await assertFails(
    bobToAliceNotif.doc("n_follow_bad").set({
      recipientUserId: ALICE_UID,
      actorUserId: BOB_UID,
      actorName: "सहर",
      type: "follow",
      postId: "invalid_post",
      postExcerpt: "कवि प्रोफ़ाइल",
      commentText: "",
      isRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobToAliceNotif.doc("n_follow_ok").set({
      recipientUserId: ALICE_UID,
      actorUserId: BOB_UID,
      actorName: "सहर",
      type: "follow",
      postId: "profile",
      postExcerpt: "सहर",
      commentText: "",
      isRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Atomic unfollow batch succeeds
  const unfollowBatch = bobDb.batch();
  unfollowBatch.delete(bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID));
  unfollowBatch.delete(bobDb.collection("poet_profiles").doc(ALICE_UID).collection("followers").doc(BOB_UID));
  unfollowBatch.update(bobDb.collection("poet_profiles").doc(ALICE_UID), { followerCount: 0 });
  unfollowBatch.update(bobDb.collection("poet_profiles").doc(BOB_UID), {
    followingCount: 0,
    updatedAt: new Date(),
  });
  await assertSucceeds(unfollowBatch.commit());
});

test("Community shayari supports joke and extended categories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  for (const catId of ["attitude", "tareef", "jija_sali", "mulla_nasruddin", "santa_banta", "motu_patlu"]) {
    await assertSucceeds(
      aliceDb
        .collection("community_shayari")
        .doc(`ext_${catId}`)
        .set(validShayariPayload(ALICE_UID, { categoryId: catId }))
    );
  }
});
