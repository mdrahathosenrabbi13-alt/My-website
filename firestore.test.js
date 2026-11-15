const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_EMAIL = "mdrahathosenrabbi13@gmail.com";
const ADMIN_UID = "admin_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 2000);
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

test("Unauthenticated user cannot read products, orders, or profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User profile isolation and shadow update rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Rahman",
      email: "alice@example.com",
      phone: "01787962913",
      preferredLanguage: "en",
      notificationsEnabled: true,
      promoNotificationsEnabled: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob cannot read Alice's PII profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice Updated",
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );
});

test("User cart and wishlist strictly scoped to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("item_1").set({
      userId: ALICE_UID,
      productId: "prod_1",
      productName: "Jamdani Saree",
      unitPrice: 4500,
      regularPrice: 5500,
      quantity: 2,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );
});

test("Products can be read by signed-in user, written only by verified admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const productPayload = {
    name: "Premium Wireless Headphones",
    nameBn: "প্রিমিয়াম ওয়্যারলেস হেডফোন",
    description: "Studio sound quality",
    categoryId: "cat_electronics",
    categoryName: "Electronics",
    brand: "SoundBD",
    regularPrice: 3500,
    discountPrice: 2890,
    stock: 25,
    imageResNames: ["img_prod_headphones"],
    sizes: ["Standard"],
    colors: ["Matte Black"],
    isFeatured: true,
    isNewArrival: true,
    isBestSeller: false,
    isFlashSale: true,
    rating: 4.8,
    reviewCount: 12,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };

  // Regular user and unverified email cannot create product
  await assertFails(aliceDb.collection("products").doc("prod_1").set(productPayload));
  await assertFails(spoofedAdminDb.collection("products").doc("prod_1").set(productPayload));

  // Verified admin succeeds
  await assertSucceeds(adminDb.collection("products").doc("prod_1").set(productPayload));

  // Signed-in user can read
  await assertSucceeds(aliceDb.collection("products").get());
});

test("Orders enforce userId ownership, query filtering, and status locks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const orderPayload = {
    userId: ALICE_UID,
    customerName: "Alice Rahman",
    customerPhone: "01787962913",
    customerEmail: "alice@example.com",
    district: "Gaibandha",
    upazila: "Gaibandha Sadar",
    area: "Station Road",
    fullAddress: "House 12, Station Road, Gaibandha",
    orderNotes: "Call before delivery",
    items: [
      {
        productId: "prod_1",
        productName: "Headphones",
        unitPrice: 2890,
        quantity: 1,
        selectedSize: "Standard",
        selectedColor: "Black",
      },
    ],
    subtotal: 2890,
    deliveryCharge: 120,
    couponCode: "",
    couponDiscount: 0,
    totalAmount: 3010,
    paymentMethod: "bKash",
    paymentStatus: "Awaiting Verification",
    transactionId: "TRX98765432",
    status: "Order Placed",
    estimatedDelivery: "2-3 Days",
    trackingHistory: [
      { stage: "Order Placed", note: "Order received", timestampLabel: "Just now" },
    ],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };

  await assertSucceeds(aliceDb.collection("orders").doc("ord_1001").set(orderPayload));

  // Unfiltered list fails for non-admin
  await assertFails(aliceDb.collection("orders").get());

  // Filtered list succeeds for owner
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );

  // Cross-user read fails
  await assertFails(bobDb.collection("orders").doc("ord_1001").get());

  // Customer cannot self-approve payment or jump status to Delivered
  await assertFails(
    aliceDb.collection("orders").doc("ord_1001").update({
      status: "Delivered",
      updatedAt: nowTs(),
    })
  );

  // Customer can cancel eligible order
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1001").update({
      status: "Cancelled",
      trackingHistory: [
        { stage: "Order Placed", note: "Order received", timestampLabel: "Just now" },
        { stage: "Cancelled", note: "Cancelled by customer", timestampLabel: "Now" },
      ],
      updatedAt: nowTs(),
    })
  );

  // Admin can list all orders and update any stage
  await assertSucceeds(adminDb.collection("orders").get());
  await assertSucceeds(
    adminDb.collection("orders").doc("ord_1001").update({
      status: "Order Confirmed",
      paymentStatus: "Paid",
      updatedAt: nowTs(),
    })
  );
});
