const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("IdentityRegistry", function () {
  async function fixture() {
    const [admin, manager, alice, bob] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("IdentityRegistry");
    const registry = await Factory.deploy(admin.address);
    await registry.waitForDeployment();
    const managerRole = await registry.IDENTITY_MANAGER_ROLE();
    await registry.grantRole(managerRole, manager.address);
    return { registry, admin, manager, alice, bob };
  }

  it("grants admin and manager permissions to the initial admin", async function () {
    const { registry, admin } = await fixture();
    expect(await registry.hasRole(await registry.DEFAULT_ADMIN_ROLE(), admin.address)).to.equal(true);
    expect(await registry.hasRole(await registry.IDENTITY_MANAGER_ROLE(), admin.address)).to.equal(true);
  });

  it("restricts manager grants and revocations to the default admin", async function () {
    const { registry, admin, alice } = await fixture();
    const managerRole = await registry.IDENTITY_MANAGER_ROLE();
    await expect(registry.connect(alice).grantRole(managerRole, alice.address)).to.be.reverted;
    await registry.connect(admin).grantRole(managerRole, alice.address);
    expect(await registry.hasRole(managerRole, alice.address)).to.equal(true);
    await registry.connect(admin).revokeRole(managerRole, alice.address);
    expect(await registry.hasRole(managerRole, alice.address)).to.equal(false);
  });

  it("registers a new wallet and emits the registration event", async function () {
    const { registry, manager, alice } = await fixture();
    await expect(registry.connect(manager).registerIdentity(alice.address, 0))
      .to.emit(registry, "IdentityRegistered");
    expect(await registry.isRegistered(alice.address)).to.equal(true);
    expect(await registry.isActive(alice.address)).to.equal(true);
    expect(await registry.getRole(alice.address)).to.equal(0);
  });

  it("rejects duplicate registrations", async function () {
    const { registry, manager, alice } = await fixture();
    await registry.connect(manager).registerIdentity(alice.address, 0);
    await expect(registry.connect(manager).registerIdentity(alice.address, 0))
      .to.be.revertedWith("identity already registered");
  });

  it("rejects registration by an unauthorized wallet", async function () {
    const { registry, alice, bob } = await fixture();
    await expect(registry.connect(alice).registerIdentity(bob.address, 0))
      .to.be.reverted;
  });

  it("allows only the default admin to assign roles", async function () {
    const { registry, admin, manager, alice } = await fixture();
    await registry.connect(manager).registerIdentity(alice.address, 0);
    await expect(registry.connect(alice).assignRole(alice.address, 1)).to.be.reverted;
    await expect(registry.connect(manager).assignRole(alice.address, 1))
      .to.emit(registry, "IdentityRoleUpdated");
    expect(await registry.getRole(alice.address)).to.equal(1);
  });

  it("deactivates and reactivates an identity through manager access", async function () {
    const { registry, manager, alice } = await fixture();
    await registry.connect(manager).registerIdentity(alice.address, 0);
    await expect(registry.connect(manager).deactivateIdentity(alice.address))
      .to.emit(registry, "IdentityDeactivated");
    expect(await registry.isActive(alice.address)).to.equal(false);
    await expect(registry.connect(manager).reactivateIdentity(alice.address))
      .to.emit(registry, "IdentityReactivated");
    expect(await registry.isActive(alice.address)).to.equal(true);
  });

  it("rejects assigning a role to an inactive identity", async function () {
    const { registry, admin, manager, alice } = await fixture();
    await registry.connect(manager).registerIdentity(alice.address, 0);
    await registry.connect(manager).deactivateIdentity(alice.address);
    await expect(registry.connect(admin).assignRole(alice.address, 2))
      .to.be.revertedWith("identity is inactive");
  });

  it("returns identity details", async function () {
    const { registry, manager, alice } = await fixture();
    await registry.connect(manager).registerIdentity(alice.address, 2);
    const identity = await registry.getIdentity(alice.address);
    expect(identity.registered).to.equal(true);
    expect(identity.active).to.equal(true);
    expect(identity.role).to.equal(2);
  });
});
