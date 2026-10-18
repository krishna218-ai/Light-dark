# Security Specification (`security_spec.md`)

## 1. Data Invariants
- **Strict Multi-Tenant Ownership**: Every `ConfigBackup` document lives at `/users/{userId}/configBackups/{backupId}` and MUST have `data.userId == request.auth.uid` and `request.auth.uid == userId`.
- **No Unauthenticated Access**: Unauthenticated requests (`request.auth == null`) are rejected across all operations (`get`, `list`, `create`, `update`, `delete`).
- **Path Variable Hardening**: `userId` and `backupId` must match `^[a-zA-Z0-9_\-]+$` with length `1..128` on single-document operations (`get`, `create`, `update`, `delete`).
- **Strict Schema & Bounds**:
  - `title`: string, `1..120` chars
  - `description`: string, `0..500` chars
  - `configJson`: string, `2..200000` chars
  - `accentPreset`: string, `1..60` chars
  - `moduleCount`: int, `0..100`
  - `addonCount`: int, `0..200`
  - `createdAt`, `updatedAt`: `timestamp <= request.time`
- **Immortal Fields on Update**: `userId` and `createdAt` cannot be mutated during an `update`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/configBackups").get()` -> Reject
2. **Cross-User Read**: `bobDb.doc("users/alice/configBackups/b1").get()` -> Reject
3. **Cross-User List**: `bobDb.collection("users/alice/configBackups").where("userId", "==", "alice").get()` -> Reject
4. **Spoofed Owner Field**: `aliceDb` writing `userId: "bob"` under `/users/alice/configBackups/b1` -> Reject
5. **Shadow / Ghost Field Injection on Create**: Payload containing `isAdmin: true` -> Reject via `hasOnly`
6. **Shadow / Ghost Field Injection on Update**: Update adding `isVerified: true` -> Reject via `affectedKeys().hasOnly`
7. **Immortal Field Mutation (`createdAt`)**: Update changing `createdAt` -> Reject
8. **Immortal Field Mutation (`userId`)**: Update changing `userId` -> Reject
9. **Oversized Title / Denial of Wallet**: `title` > 120 chars -> Reject
10. **Negative or Out-of-Range Counts**: `moduleCount: -5` or `moduleCount: 999` -> Reject
11. **Future Timestamp Spoofing**: `createdAt` set in the future (`> request.time`) -> Reject
12. **Unfiltered List Query**: `aliceDb.collection("users/alice/configBackups").get()` without `where("userId", "==", "alice")` when document has mismatched userId -> Enforced by `existing().userId == request.auth.uid`
