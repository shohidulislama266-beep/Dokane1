const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_123";
const STAFF_UID = "staff_456";
const STAFF2_UID = "staff_789";
const OUTSIDER_UID = "outsider_999";
const STORE_ID = "store_abc";
const OTHER_STORE_ID = "store_xyz";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const OWNER_PERMS = {
  sell: true,
  view_stock: true,
  add_edit_products: true,
  see_buying_price_and_profit: true,
  history: true,
  customers_and_baki: true,
  receive_payment: true,
  suppliers_and_purchases: true,
  pay_supplier: true,
  reports: true,
  cash_and_bank: true,
  expenses_and_other_income: true,
  notes: true,
};

const LIMITED_PERMS = {
  sell: true,
  view_stock: true,
  add_edit_products: false,
  see_buying_price_and_profit: false,
  history: false,
  customers_and_baki: true,
  receive_payment: false,
  suppliers_and_purchases: false,
  pay_supplier: false,
  reports: false,
  cash_and_bank: false,
  expenses_and_other_income: false,
  notes: true,
};

const FULL_STAFF_PERMS = { ...OWNER_PERMS };

function buildStorePayload(ownerUid, name = "Bismillah Store") {
  const now = new Date();
  return {
    name,
    phone: "01712345678",
    address: "Dhaka",
    shopType: "grocery",
    productCategories: ["Grocery", "Beverages"],
    ownerId: ownerUid,
    memberUids: [ownerUid],
    members: [{ userId: ownerUid, role: "owner", status: "active" }],
    createdAt: now,
    updatedAt: now,
  };
}

function buildOwnerMemberPayload(ownerUid) {
  return {
    userId: ownerUid,
    displayName: "Owner Rahim",
    email: "owner@store.com",
    role: "owner",
    roleLabel: "Owner",
    permissionLevel: "Full",
    permissions: OWNER_PERMS,
    status: "Active",
    joinedTime: Date.now(),
    invitedBy: "",
    inviteCode: "",
    updatedAt: new Date(),
  };
}

function buildInvitePayload({
  code = "K7M9P2XQ",
  storeId = STORE_ID,
  createdByUid = OWNER_UID,
  status = "active",
  roleLabel = "Cashier",
  permissionLevel = "Limited",
  permissions = LIMITED_PERMS,
  expiresAt = Date.now() + 24 * 60 * 60 * 1000,
  usedByUid = "",
  usedByEmail = "",
  usedAt = 0,
} = {}) {
  return {
    code,
    storeId,
    storeName: "Bismillah Store",
    staffName: "Karim",
    roleLabel,
    permissionLevel,
    permissions,
    createdByUid,
    createdByName: "Owner Rahim",
    status,
    createdAt: Date.now(),
    expiresAt,
    usedByUid,
    usedByEmail,
    usedAt,
  };
}

async function seedStoreWithOwnerAndStaff({ staffStatus = "Active", staffInMemberUids = true } = {}) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const memberUids = staffInMemberUids ? [OWNER_UID, STAFF_UID] : [OWNER_UID];
    await db.collection("stores").doc(STORE_ID).set({
      ...buildStorePayload(OWNER_UID),
      memberUids,
    });
    await db
      .collection("stores")
      .doc(STORE_ID)
      .collection("members")
      .doc(OWNER_UID)
      .set(buildOwnerMemberPayload(OWNER_UID));
    await db
      .collection("stores")
      .doc(STORE_ID)
      .collection("members")
      .doc(STAFF_UID)
      .set({
        userId: STAFF_UID,
        displayName: "Karim",
        email: "staff@store.com",
        role: "cashier",
        roleLabel: "Cashier",
        permissionLevel: "Limited",
        permissions: LIMITED_PERMS,
        status: staffStatus,
        joinedTime: Date.now(),
        invitedBy: OWNER_UID,
        inviteCode: "SEEDED01",
      });
    await db
      .collection("stores")
      .doc(STORE_ID)
      .collection("meta")
      .doc("counters")
      .set({ invoiceSeq: 100 });
    await db
      .collection("stores")
      .doc(STORE_ID)
      .collection("products")
      .doc("prod_rice")
      .set({
        storeId: STORE_ID,
        name: "Miniket Rice 5kg",
        unit: "pcs",
        buyPrice: 320,
        sellPrice: 375,
        stock: 20,
      });
  });
}

async function seedStoreOwnerOnly(storeId = STORE_ID, ownerUid = OWNER_UID) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("stores").doc(storeId).set(buildStorePayload(ownerUid));
    await db
      .collection("stores")
      .doc(storeId)
      .collection("members")
      .doc(ownerUid)
      .set(buildOwnerMemberPayload(ownerUid));
    await db
      .collection("stores")
      .doc(storeId)
      .collection("meta")
      .doc("counters")
      .set({ invoiceSeq: 100 });
  });
}

function buildJoinBatch(db, {
  code,
  storeId = STORE_ID,
  uid = STAFF_UID,
  email = "staff@store.com",
  displayName = "Karim",
  roleLabel = "Cashier",
  role = "cashier",
  permissionLevel = "Limited",
  permissions = LIMITED_PERMS,
  invitedBy = OWNER_UID,
  nextMemberUids = [OWNER_UID, STAFF_UID],
}) {
  const now = Date.now();
  const batch = db.batch();
  const inviteRef = db.collection("invites").doc(code);
  const storeRef = db.collection("stores").doc(storeId);
  const memberRef = storeRef.collection("members").doc(uid);
  const userRef = db.collection("users").doc(uid);

  batch.update(inviteRef, {
    status: "used",
    usedByUid: uid,
    usedByEmail: email,
    usedAt: now,
    updatedAt: new Date(),
  });
  batch.set(memberRef, {
    userId: uid,
    displayName,
    email,
    role,
    roleLabel,
    permissionLevel,
    permissions,
    status: "Active",
    joinedTime: now,
    invitedBy,
    inviteCode: code,
  });
  batch.update(storeRef, {
    memberUids: nextMemberUids,
    updatedAt: new Date(),
  });
  batch.set(
    userRef,
    {
      email,
      storeId,
      role: roleLabel,
      updatedAt: new Date(),
    },
    { merge: true }
  );
  return batch;
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

test("1. New store setup succeeds in a single atomic batch with no PERMISSION_DENIED", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

  // Single atomic batch: stores/{id}, stores/{id}/members/{uid}, users/{uid}, stores/{id}/meta/counters
  const batch = ownerDb.batch();
  const storeRef = ownerDb.collection("stores").doc(STORE_ID);
  const ownerMemberRef = storeRef.collection("members").doc(OWNER_UID);
  const userRef = ownerDb.collection("users").doc(OWNER_UID);
  const counterRef = storeRef.collection("meta").doc("counters");

  batch.set(storeRef, buildStorePayload(OWNER_UID));
  batch.set(ownerMemberRef, buildOwnerMemberPayload(OWNER_UID));
  batch.set(
    userRef,
    {
      email: "owner@store.com",
      storeId: STORE_ID,
      role: "owner",
      createdAt: new Date(),
      updatedAt: new Date(),
    },
    { merge: true }
  );
  batch.set(counterRef, { invoiceSeq: 100 }, { merge: true });

  await assertSucceeds(batch.commit());

  // Also verify the sequential write sequence used in StoreRepository.createStore passes
  const seqStoreId = "store_seq_01";
  const seqStoreRef = ownerDb.collection("stores").doc(seqStoreId);
  await assertSucceeds(seqStoreRef.set(buildStorePayload(OWNER_UID, "Sequential Shop")));
  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).set(
      {
        email: "owner@store.com",
        storeId: seqStoreId,
        role: "owner",
        updatedAt: new Date(),
      },
      { merge: true }
    )
  );
  await assertSucceeds(
    seqStoreRef.collection("members").doc(OWNER_UID).set(buildOwnerMemberPayload(OWNER_UID))
  );
  await assertSucceeds(
    seqStoreRef.collection("meta").doc("counters").set({ invoiceSeq: 100 }, { merge: true })
  );
});

test("2. Owner creates an invite and lists own invites; non-owners cannot create or list", async () => {
  await seedStoreWithOwnerAndStaff();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  // Owner creates a valid invite (exact StoreRepository.createStaffInvite shape)
  await assertSucceeds(
    ownerDb.collection("invites").doc("K7M9P2XQ").set(buildInvitePayload({ code: "K7M9P2XQ" }))
  );

  // Owner cannot create an invite with roleLabel "Owner"
  await assertFails(
    ownerDb
      .collection("invites")
      .doc("OWNR1234")
      .set(buildInvitePayload({ code: "OWNR1234", roleLabel: "Owner" }))
  );

  // Staff and outsider cannot create invites
  await assertFails(
    staffDb
      .collection("invites")
      .doc("STFF1234")
      .set(buildInvitePayload({ code: "STFF1234", createdByUid: STAFF_UID }))
  );
  await assertFails(
    outsiderDb
      .collection("invites")
      .doc("OUTS1234")
      .set(buildInvitePayload({ code: "OUTS1234", createdByUid: OUTSIDER_UID }))
  );

  // Creating owner can list own invites (StoreRepository.observeInvites query)
  await assertSucceeds(
    ownerDb
      .collection("invites")
      .where("storeId", "==", STORE_ID)
      .where("createdByUid", "==", OWNER_UID)
      .get()
  );

  // Unfiltered list and non-owner list are denied
  await assertFails(ownerDb.collection("invites").get());
  await assertFails(
    staffDb.collection("invites").where("storeId", "==", STORE_ID).get()
  );
  await assertFails(
    outsiderDb.collection("invites").where("createdByUid", "==", OWNER_UID).get()
  );

  // Any signed-in user can get one invite by exact code
  await assertSucceeds(outsiderDb.collection("invites").doc("K7M9P2XQ").get());
});

test("3. Staff joins with a valid invite; tampered role, permissions, or memberUids are denied", async () => {
  await seedStoreOwnerOnly();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const staff2Db = testEnv.authenticatedContext(STAFF2_UID).firestore();

  await assertSucceeds(
    ownerDb.collection("invites").doc("JOIN0001").set(buildInvitePayload({ code: "JOIN0001" }))
  );

  // Tampering: trying to promote self to "owner" during join -> DENIED
  await assertFails(
    buildJoinBatch(staffDb, {
      code: "JOIN0001",
      role: "owner",
      roleLabel: "Owner",
      permissions: OWNER_PERMS,
    }).commit()
  );

  // Tampering: trying to inflate permissions beyond the invite -> DENIED
  await assertFails(
    buildJoinBatch(staffDb, {
      code: "JOIN0001",
      permissions: { ...LIMITED_PERMS, reports: true },
    }).commit()
  );

  // Tampering: trying to add an extra UID to memberUids -> DENIED
  await assertFails(
    buildJoinBatch(staffDb, {
      code: "JOIN0001",
      nextMemberUids: [OWNER_UID, STAFF_UID, OUTSIDER_UID],
    }).commit()
  );

  // Tampering: trying to use invite for a different store -> DENIED
  await seedStoreOwnerOnly(OTHER_STORE_ID, OWNER_UID);
  await assertFails(
    buildJoinBatch(staffDb, {
      code: "JOIN0001",
      storeId: OTHER_STORE_ID,
      nextMemberUids: [OWNER_UID, STAFF_UID],
    }).commit()
  );

  // Valid atomic join matching StoreRepository.joinStoreWithInvite -> SUCCEEDS
  await assertSucceeds(buildJoinBatch(staffDb, { code: "JOIN0001" }).commit());

  // Joined active staff can now read the store and operational data
  await assertSucceeds(staffDb.collection("stores").doc(STORE_ID).get());
  await assertSucceeds(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID).get()
  );

  // Also verify "pending" status invites work for joining
  await assertSucceeds(
    ownerDb
      .collection("invites")
      .doc("PEND0002")
      .set(buildInvitePayload({ code: "PEND0002", status: "pending" }))
  );
  await assertSucceeds(
    buildJoinBatch(staff2Db, {
      code: "PEND0002",
      uid: STAFF2_UID,
      email: "staff2@store.com",
      displayName: "Rahim",
      nextMemberUids: [OWNER_UID, STAFF_UID, STAFF2_UID],
    }).commit()
  );
});

test("4. The same invite used twice is denied", async () => {
  await seedStoreOwnerOnly();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const staff2Db = testEnv.authenticatedContext(STAFF2_UID).firestore();

  await assertSucceeds(
    ownerDb.collection("invites").doc("ONCE1234").set(buildInvitePayload({ code: "ONCE1234" }))
  );

  // First staff joins cleanly
  await assertSucceeds(buildJoinBatch(staffDb, { code: "ONCE1234" }).commit());

  // Second staff attempting to reuse the same invite is denied
  await assertFails(
    buildJoinBatch(staff2Db, {
      code: "ONCE1234",
      uid: STAFF2_UID,
      email: "staff2@store.com",
      displayName: "Duplicate User",
      nextMemberUids: [OWNER_UID, STAFF_UID, STAFF2_UID],
    }).commit()
  );

  // Owner cannot reopen or delete a used invite
  await assertFails(
    ownerDb.collection("invites").doc("ONCE1234").update({ status: "active", updatedAt: new Date() })
  );
  await assertFails(ownerDb.collection("invites").doc("ONCE1234").delete());
});

test("5. Expired and cancelled invites are denied", async () => {
  await seedStoreOwnerOnly();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  // Seed an expired invite (past expiresAt)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("invites")
      .doc("EXPD1234")
      .set(
        buildInvitePayload({
          code: "EXPD1234",
          expiresAt: Date.now() - 60 * 1000,
        })
      );
  });

  // Joining with an expired invite is denied
  await assertFails(buildJoinBatch(staffDb, { code: "EXPD1234" }).commit());

  // Owner creates an active invite, then cancels it (exact StoreRepository.cancelInvite write)
  await assertSucceeds(
    ownerDb.collection("invites").doc("CNCL1234").set(buildInvitePayload({ code: "CNCL1234" }))
  );
  await assertSucceeds(
    ownerDb.collection("invites").doc("CNCL1234").update({
      status: "cancelled",
      updatedAt: new Date(),
    })
  );

  // Joining with a cancelled invite is denied
  await assertFails(buildJoinBatch(staffDb, { code: "CNCL1234" }).commit());

  // Owner cannot reactivate a cancelled invite
  await assertFails(
    ownerDb.collection("invites").doc("CNCL1234").update({
      status: "active",
      updatedAt: new Date(),
    })
  );
});

test("6. A member cannot add another member, change roles/self, or edit store settings", async () => {
  await seedStoreWithOwnerAndStaff();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  // Staff cannot add another member
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF2_UID).set({
      userId: STAFF2_UID,
      displayName: "Unauthorized Staff",
      email: "s2@store.com",
      role: "sales",
      roleLabel: "Sales",
      permissionLevel: "Limited",
      permissions: LIMITED_PERMS,
      status: "Active",
      joinedTime: Date.now(),
      invitedBy: STAFF_UID,
      inviteCode: "",
    })
  );

  // Staff cannot edit own member record or promote self to owner
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID).update({
      role: "owner",
      roleLabel: "Owner",
      permissions: OWNER_PERMS,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID).update({
      displayName: "Renamed Self",
      updatedAt: new Date(),
    })
  );

  // Staff cannot edit owner's member record
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(OWNER_UID).update({
      status: "Removed",
      updatedAt: new Date(),
    })
  );

  // Staff cannot change store name, settings, or shopType
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).update({
      name: "Hijacked Shop",
      shopType: "pharmacy",
      updatedAt: new Date(),
    })
  );

  // Owner cannot edit their own member record either (strict Requirement 4)
  await assertFails(
    ownerDb.collection("stores").doc(STORE_ID).collection("members").doc(OWNER_UID).update({
      displayName: "Self Edit",
      updatedAt: new Date(),
    })
  );

  // Owner CAN update staff permissions, change store settings, remove and restore staff
  await assertSucceeds(
    ownerDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("members")
      .doc(STAFF_UID)
      .set(
        {
          roleLabel: "Manager",
          role: "manager",
          permissionLevel: "Full",
          permissions: FULL_STAFF_PERMS,
          updatedAt: new Date(),
        },
        { merge: true }
      )
  );

  // Owner cannot promote a staff member to "owner"
  await assertFails(
    ownerDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("members")
      .doc(STAFF_UID)
      .update({
        role: "owner",
        roleLabel: "Owner",
        updatedAt: new Date(),
      })
  );

  // Owner CAN update store shopType & settings (StoreRepository.updateStoreShopType)
  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).update({
      name: "Bismillah Mart",
      shopType: "super_shop",
      productCategories: ["Grocery", "Beverages", "Dairy"],
      updatedAt: new Date(),
    })
  );
});

test("7. A non-member cannot read or write store data", async () => {
  await seedStoreWithOwnerAndStaff();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  await assertFails(outsiderDb.collection("stores").doc(STORE_ID).get());
  await assertFails(outsiderDb.collection("stores").get());
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("members").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("members").doc(OWNER_UID).get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("sales").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("meta").doc("counters").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("products").doc("p_hack").set({
      storeId: STORE_ID,
      name: "Injected",
    })
  );
});

test("8. Staff cannot read or write the AI key; owner can", async () => {
  await seedStoreWithOwnerAndStaff();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  const keyPayload = {
    storeId: STORE_ID,
    apiKey: "AIzaSyTestSecretKey123456",
    keyLast4: "3456",
    updatedBy: OWNER_UID,
    updatedAt: new Date(),
  };

  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").set(keyPayload)
  );
  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").get()
  );

  // Active staff denied get, list, set, update, delete on private/aiKey
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("private").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").set({
      ...keyPayload,
      updatedBy: STAFF_UID,
    })
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").delete()
  );

  // Owner can delete AI key
  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").delete()
  );
});

test("9. A removed member is denied everything", async () => {
  await seedStoreWithOwnerAndStaff();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  // Owner executes the real removeMember atomic sequence (status -> "Removed" + drop from memberUids)
  const removeBatch = ownerDb.batch();
  removeBatch.update(
    ownerDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID),
    { status: "Removed", updatedAt: new Date() }
  );
  removeBatch.update(ownerDb.collection("stores").doc(STORE_ID), {
    memberUids: [OWNER_UID],
    updatedAt: new Date(),
  });
  await assertSucceeds(removeBatch.commit());

  // Removed member is denied EVERYTHING on the store
  await assertFails(staffDb.collection("stores").doc(STORE_ID).get());
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID).get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_rice").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_new").set({
      storeId: STORE_ID,
      name: "Blocked",
    })
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("sales").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").get()
  );

  // Even if stale memberUids still contained the removed UID, status "Removed" still denies everything
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("stores")
      .doc(STORE_ID)
      .update({ memberUids: [OWNER_UID, STAFF_UID] });
  });
  await assertFails(staffDb.collection("stores").doc(STORE_ID).get());
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID).get()
  );

  // Removed member cannot restore themselves
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID).update({
      status: "Active",
      updatedAt: new Date(),
    })
  );

  // Removed member cannot self-rejoin even with a fresh invite (only owner can restore)
  await assertSucceeds(
    ownerDb.collection("invites").doc("REJN1234").set(buildInvitePayload({ code: "REJN1234" }))
  );
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("stores")
      .doc(STORE_ID)
      .update({ memberUids: [OWNER_UID] });
  });
  await assertFails(buildJoinBatch(staffDb, { code: "REJN1234" }).commit());

  // Owner CAN restore the removed member (StoreRepository.restoreMember)
  const restoreBatch = ownerDb.batch();
  restoreBatch.update(
    ownerDb.collection("stores").doc(STORE_ID).collection("members").doc(STAFF_UID),
    { status: "Active", updatedAt: new Date() }
  );
  restoreBatch.update(ownerDb.collection("stores").doc(STORE_ID), {
    memberUids: [OWNER_UID, STAFF_UID],
    updatedAt: new Date(),
  });
  await assertSucceeds(restoreBatch.commit());
  await assertSucceeds(staffDb.collection("stores").doc(STORE_ID).get());
});

test("10. A signed-out user is denied everything", async () => {
  await seedStoreWithOwnerAndStaff();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("invites")
      .doc("OPEN1234")
      .set(buildInvitePayload({ code: "OPEN1234" }));
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauthDb.collection("users").doc(OWNER_UID).get());
  await assertFails(unauthDb.collection("users").doc("anon").set({ email: "a@b.com" }));
  await assertFails(unauthDb.collection("invites").doc("OPEN1234").get());
  await assertFails(unauthDb.collection("invites").get());
  await assertFails(unauthDb.collection("stores").doc(STORE_ID).get());
  await assertFails(unauthDb.collection("stores").get());
  await assertFails(
    unauthDb.collection("stores").doc("new_s").set(buildStorePayload("anon"))
  );
  await assertFails(
    unauthDb.collection("stores").doc(STORE_ID).collection("members").get()
  );
  await assertFails(
    unauthDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(
    unauthDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").get()
  );
});

test("11. A normal sale with a stock update works for the owner and active staff", async () => {
  await seedStoreWithOwnerAndStaff();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  // Exact StoreRepository.executeSaleCheckout write sequence for Owner
  const ownerSaleBatch = ownerDb.batch();
  const storeRef = ownerDb.collection("stores").doc(STORE_ID);
  ownerSaleBatch.set(
    storeRef.collection("meta").doc("counters"),
    { invoiceSeq: 101 },
    { merge: true }
  );
  ownerSaleBatch.update(storeRef.collection("products").doc("prod_rice"), {
    stock: 18,
    updatedAt: new Date(),
  });
  ownerSaleBatch.set(storeRef.collection("sales").doc("sale_101"), {
    storeId: STORE_ID,
    invoiceNo: "INV-0101",
    items: [
      {
        productId: "prod_rice",
        name: "Miniket Rice 5kg",
        qty: 2,
        unit: "pcs",
        stockQty: 2,
        buyPrice: 320,
        sellPrice: 375,
        discount: 0,
        lineTotal: 750,
        profit: 110,
      },
    ],
    totalItems: 2,
    subtotal: 750,
    discount: 0,
    grandTotal: 750,
    paidAmount: 750,
    dueAmount: 0,
    profit: 110,
    paymentType: "cash",
    customerId: "",
    customerName: "Cash Sale",
    customerPhone: "",
    accountId: "cash_in_hand",
    accountName: "Cash in Hand",
    notes: "",
    soldBy: OWNER_UID,
    createdAt: new Date(),
  });
  await assertSucceeds(ownerSaleBatch.commit());

  // Also verify an active staff member can perform the same normal sale + stock update
  const staffSaleBatch = staffDb.batch();
  const staffStoreRef = staffDb.collection("stores").doc(STORE_ID);
  staffSaleBatch.set(
    staffStoreRef.collection("meta").doc("counters"),
    { invoiceSeq: 102 },
    { merge: true }
  );
  staffSaleBatch.update(staffStoreRef.collection("products").doc("prod_rice"), {
    stock: 17,
    updatedAt: new Date(),
  });
  staffSaleBatch.set(staffStoreRef.collection("sales").doc("sale_102"), {
    storeId: STORE_ID,
    invoiceNo: "INV-0102",
    items: [
      {
        productId: "prod_rice",
        name: "Miniket Rice 5kg",
        qty: 1,
        unit: "pcs",
        stockQty: 1,
        buyPrice: 320,
        sellPrice: 375,
        discount: 0,
        lineTotal: 375,
        profit: 55,
      },
    ],
    totalItems: 1,
    subtotal: 375,
    discount: 0,
    grandTotal: 375,
    paidAmount: 375,
    dueAmount: 0,
    profit: 55,
    paymentType: "cash",
    customerId: "",
    customerName: "Cash Sale",
    customerPhone: "",
    accountId: "cash_in_hand",
    accountName: "Cash in Hand",
    notes: "",
    soldBy: STAFF_UID,
    createdAt: new Date(),
  });
  await assertSucceeds(staffSaleBatch.commit());
});
