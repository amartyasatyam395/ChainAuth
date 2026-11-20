require("@nomicfoundation/hardhat-toolbox");
module.exports = {
  solidity: { version: "0.8.28", settings: { optimizer: { enabled: true, runs: 200 } } },
  networks: { localhost: { url: process.env.ETHEREUM_RPC_URL || "http://127.0.0.1:8545", chainId: Number(process.env.ETHEREUM_CHAIN_ID || 31337) } }
};
