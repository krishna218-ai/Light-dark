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

function validBackupPayload(userId, overrides = {}) {
  const ts = new Date(Date.now() - 1000);
  return {
    userId,
    title: "Hive PvP Cloud Backup",
    description: "7 modules active, Cyber Cyan theme",
    configJson: "{\"version\":2,\"activeProfileId\":\"profile_pvp\"}",
    accentPreset: "CYBER_CYAN",
    moduleCount: 5,
    addonCount: 3,
    createdAt: ts,
    updatedAt: ts,
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

test("Unauthenticated user: cannot read or create config backups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("configBackups").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("configBackups")
      .doc("backup_1")
      .set(validBackupPayload(ALICE_UID))
  );
});

test("Authenticated user: can create, get, list, update, and delete own config backup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("configBackups")
    .doc("backup_1");

  await assertSucceeds(docRef.set(validBackupPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("configBackups")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    docRef.update({
      title: "Updated Hive Backup",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Authenticated user: cannot read, update, or delete another user's config backup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("configBackups")
      .doc("backup_alice")
      .set(validBackupPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDocAsBob = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("configBackups")
    .doc("backup_alice");

  await assertFails(aliceDocAsBob.get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("configBackups")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertFails(
    aliceDocAsBob.update({
      title: "Hacked by Bob",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(aliceDocAsBob.delete());
});

test("Shadow field injection and invalid schema payloads are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("configBackups");

  // Ghost field on create
  await assertFails(
    colRef.doc("b_ghost").set(validBackupPayload(ALICE_UID, { isVerified: true }))
  );

  // Spoofed userId
  await assertFails(
    colRef.doc("b_spoof").set(validBackupPayload(BOB_UID))
  );

  // Empty title
  await assertFails(
    colRef.doc("b_empty_title").set(validBackupPayload(ALICE_UID, { title: "" }))
  );

  // Out-of-bounds moduleCount
  await assertFails(
    colRef.doc("b_bad_count").set(validBackupPayload(ALICE_UID, { moduleCount: 999 }))
  );
});
