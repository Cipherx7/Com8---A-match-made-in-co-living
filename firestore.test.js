const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, serverTimestamp) {
  return {
    userId: uid,
    displayName: "Aarav Mehta",
    city: "Pune",
    roleType: "SEEKING_FLAT",
    monthlyBudget: 16000,
    sleepSchedule: "BALANCED",
    cleanlinessLevel: 4,
    guestPolicy: "WEEKENDS_OK",
    foodHabit: "VEG_ONLY",
    bio: "Product designer working hybrid in Baner.",
    quizCompleted: true,
    verifiedBadge: true,
    trustScore: 92,
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
  };
}

function validListing(ownerId, isPublic, serverTimestamp) {
  return {
    ownerId,
    name: "Riya Sharma",
    age: 25,
    occupation: "SDE @ Fintech",
    roleType: "HAS_FLAT",
    city: "Bengaluru",
    locality: "Koramangala 4th Block",
    monthlyRent: 17500,
    latitude: 12.9352,
    longitude: 77.6245,
    sleepSchedule: "NIGHT_OWL",
    cleanlinessLevel: 4,
    guestPolicy: "WEEKENDS_OK",
    foodHabit: "NON_VEG_OK",
    bio: "Sunlit 2BHK with balcony and fast fiber.",
    moveInTimeline: "Immediate",
    trustScore: 94,
    verified: true,
    isPublic,
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
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

test("Unauthenticated user cannot read profiles, swipes, or listings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("swipes").get());
  await assertFails(unauthDb.collection("listings").where("isPublic", "==", true).get());
});

test("User can create and read own lifestyle profile, but cannot read or overwrite another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const serverTimestamp = new Date();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, serverTimestamp)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).set(validProfile(BOB_UID, serverTimestamp)));
});

test("Shadow update with ghost field on profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, ts)));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      city: "Mumbai",
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("User can create swipe and message under own path, cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date();

  const swipeRef = aliceDb.collection("users").doc(ALICE_UID).collection("swipes").doc("cand_1");
  await assertSucceeds(
    swipeRef.set({
      userId: ALICE_UID,
      listingId: "cand_1",
      decision: "LIKED",
      status: "SAVED",
      targetName: "Riya",
      targetCity: "Bengaluru",
      targetLocality: "Koramangala",
      targetRent: 17500,
      compatibilityScore: 88,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    swipeRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      senderRole: "ME",
      text: "Hey! Saw our 88% match score.",
      createdAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("swipes").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("swipes").doc("cand_1").collection("messages").get()
  );
});

test("Public listings query succeeds with isPublic==true, unfiltered listing query fails, private draft hidden from others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date();

  await assertSucceeds(aliceDb.collection("listings").doc("list_pub").set(validListing(ALICE_UID, true, ts)));
  await assertSucceeds(aliceDb.collection("listings").doc("list_priv").set(validListing(ALICE_UID, false, ts)));

  await assertSucceeds(bobDb.collection("listings").where("isPublic", "==", true).get());
  await assertFails(bobDb.collection("listings").get());
  await assertFails(bobDb.collection("listings").doc("list_priv").get());
  await assertFails(bobDb.collection("listings").doc("list_pub").update({ monthlyRent: 5000, updatedAt: new Date() }));
});

test("Profile rejects invalid gender and allows valid He/She/Other with organizationName and interestsCsv", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();

  const badProfile = {
    ...validProfile(ALICE_UID, ts),
    gender: "Unknown",
  };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badProfile));

  const goodProfile = {
    ...validProfile(ALICE_UID, ts),
    gender: "She",
    organizationName: "IIT Bombay",
    interestsCsv: "Cybersecurity,Running",
  };
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(goodProfile));
});

test("Public profiles allow owner write and authenticated discoverable query, reject cross-user write or unfiltered list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date();

  const pubDoc = {
    ownerId: ALICE_UID,
    displayName: "Alice",
    age: 23,
    gender: "She",
    occupation: "Student",
    organizationName: "COEP",
    roleType: "SEEKING_FLAT",
    city: "Pune",
    locality: "Baner",
    monthlyBudget: 12000,
    smoking: "Never",
    drinking: "Never",
    sleepSchedule: "EARLY_BIRD",
    cleanlinessLevel: 4,
    guestPolicy: "WEEKENDS_OK",
    foodHabit: "VEG_ONLY",
    moveInTimeline: "Immediate",
    bio: "Looking for a tidy flatmate.",
    interestsCsv: "Reading,Startups",
    trustScore: 95,
    verified: true,
    isDiscoverable: true,
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).set(pubDoc));
  await assertSucceeds(bobDb.collection("public_profiles").where("isDiscoverable", "==", true).get());
  await assertFails(bobDb.collection("public_profiles").get());
  await assertFails(bobDb.collection("public_profiles").doc(ALICE_UID).update({ bio: "Hacked", updatedAt: new Date() }));
});

test("Swipe events and installs enforce owner-only access and immutable swipe event updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date();

  const eventRef = aliceDb.collection("users").doc(ALICE_UID).collection("swipe_events").doc("evt_1");
  await assertSucceeds(
    eventRef.set({
      userId: ALICE_UID,
      listingId: "cand_1",
      direction: "RIGHT",
      decision: "LIKED",
      targetName: "Riya",
      compatibilityScore: 91,
      createdAt: ts,
    })
  );
  await assertFails(eventRef.update({ direction: "LEFT" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("swipe_events").get());

  const installRef = aliceDb.collection("users").doc(ALICE_UID).collection("installs").doc("android_com_example");
  await assertSucceeds(
    installRef.set({
      userId: ALICE_UID,
      packageName: "com.example",
      versionName: "1.0",
      versionCode: 1,
      androidSdk: 35,
      deviceBrand: "Google",
      deviceModel: "Pixel",
      authProvider: "PHONE",
      locale: "en-IN",
      firstInstallTime: 1000,
      lastUpdateTime: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("installs").get());
});

test("Shared two-user chat allows both participants to exchange messages and blocks outsiders and sender spoofing", async () => {
  const CHARLIE_UID = "charlie_789";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date();
  const chatId = `${ALICE_UID}__${BOB_UID}`;
  const participants = [ALICE_UID, BOB_UID];

  const chatRef = aliceDb.collection("chats").doc(chatId);
  const msg1Ref = chatRef.collection("messages").doc("msg_a1");

  const batch = aliceDb.batch();
  batch.set(chatRef, {
    chatId,
    participantIds: participants,
    initiatorId: ALICE_UID,
    initiatorName: "Alice",
    listingId: `profile_${BOB_UID}`,
    targetUserId: BOB_UID,
    targetName: "Bob",
    targetCity: "Pune",
    targetLocality: "Baner",
    targetRent: 12000,
    compatibilityScore: 92,
    lastMessageText: "Hey Bob!",
    lastSenderId: ALICE_UID,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(msg1Ref, {
    messageId: "msg_a1",
    chatId,
    participantIds: participants,
    senderId: ALICE_UID,
    senderName: "Alice",
    text: "Hey Bob!",
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(
    bobDb.collection("chats").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").where("participantIds", "array-contains", BOB_UID).get()
  );

  // Bob replies atomically
  const bobBatch = bobDb.batch();
  bobBatch.update(bobDb.collection("chats").doc(chatId), {
    lastMessageText: "Hi Alice, room is open!",
    lastSenderId: BOB_UID,
    updatedAt: new Date(),
  });
  bobBatch.set(bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_b1"), {
    messageId: "msg_b1",
    chatId,
    participantIds: participants,
    senderId: BOB_UID,
    senderName: "Bob",
    text: "Hi Alice, room is open!",
    createdAt: new Date(),
  });
  await assertSucceeds(bobBatch.commit());

  // Unfiltered list queries must fail
  await assertFails(charlieDb.collection("chats").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(
    charlieDb.collection("chats").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").where("participantIds", "array-contains", ALICE_UID).get()
  );

  // Outsider cannot inject a message pretending to be Alice or Charlie
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_c1").set({
      messageId: "msg_c1",
      chatId,
      participantIds: participants,
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Spoofed!",
      createdAt: new Date(),
    })
  );
});

test("User reports allow authenticated reporter creation and own-report reads, block spoofing and cross-user reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date();
  const reportId = "rep_001";

  await assertSucceeds(
    aliceDb.collection("reports").doc(reportId).set({
      reportId,
      reporterId: ALICE_UID,
      reportedUserId: BOB_UID,
      reportedName: "Bob",
      listingId: `profile_${BOB_UID}`,
      chatId: `${ALICE_UID}__${BOB_UID}`,
      reason: "Inappropriate content",
      details: "Sent spam links in chat.",
      status: "OPEN",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("reports").where("reporterId", "==", ALICE_UID).get()
  );

  // Cross-user read and unfiltered query denied
  await assertFails(bobDb.collection("reports").doc(reportId).get());
  await assertFails(bobDb.collection("reports").get());

  // Reporter spoofing denied
  await assertFails(
    bobDb.collection("reports").doc("rep_spoof").set({
      reportId: "rep_spoof",
      reporterId: ALICE_UID,
      reportedUserId: "charlie_789",
      reportedName: "Charlie",
      listingId: "cand_1",
      chatId: "",
      reason: "Spam or scam",
      details: "",
      status: "OPEN",
      createdAt: ts,
    })
  );

  // Self-report denied
  await assertFails(
    bobDb.collection("reports").doc("rep_self").set({
      reportId: "rep_self",
      reporterId: BOB_UID,
      reportedUserId: BOB_UID,
      reportedName: "Bob",
      listingId: "cand_1",
      chatId: "",
      reason: "Other",
      details: "",
      status: "OPEN",
      createdAt: ts,
    })
  );
});


