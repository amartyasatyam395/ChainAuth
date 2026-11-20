const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  const network = await hre.ethers.provider.getNetwork();
  const Registry = await hre.ethers.getContractFactory("IdentityRegistry");
  const registry = await Registry.deploy(deployer.address);
  await registry.waitForDeployment();
  const address = await registry.getAddress();

  const backendManager = process.env.CHAINAUTH_BACKEND_SIGNER_ADDRESS;
  if (backendManager) {
    const normalized = hre.ethers.getAddress(backendManager);
    if (normalized.toLowerCase() === deployer.address.toLowerCase()) {
      console.log("Backend signer is deployer; manager role already granted.");
    } else {
      const tx = await registry.grantRole(await registry.IDENTITY_MANAGER_ROLE(), normalized);
      await tx.wait();
      console.log(`Granted IDENTITY_MANAGER_ROLE to backend signer ${normalized}`);
    }
  } else {
    console.warn("CHAINAUTH_BACKEND_SIGNER_ADDRESS not set; only deployer can manage identities.");
  }

  const artifact = await hre.artifacts.readArtifact("IdentityRegistry");
  const outputDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outputDir, { recursive: true });
  const output = {
    contractName: "IdentityRegistry",
    address,
    chainId: Number(network.chainId),
    deployer: deployer.address,
    backendManager: backendManager || null,
    deployedAt: new Date().toISOString(),
    abi: artifact.abi
  };
  fs.writeFileSync(path.join(outputDir, `${network.chainId}.json`), JSON.stringify(output, null, 2));
  console.log(`IdentityRegistry deployed at ${address} on chain ${network.chainId}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
