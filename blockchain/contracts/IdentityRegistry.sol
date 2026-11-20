// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "@openzeppelin/contracts/access/AccessControl.sol";

/**
 * @title IdentityRegistry
 * @notice Stores minimal wallet identity state and application role on-chain.
 * @dev The admin controls manager grants/revocations. Managers can register identities,
 *      assign application roles, and deactivate/reactivate identities.
 */
contract IdentityRegistry is AccessControl {
    bytes32 public constant IDENTITY_MANAGER_ROLE = keccak256("IDENTITY_MANAGER_ROLE");

    enum Role { USER, MODERATOR, ADMIN }

    struct Identity {
        bool registered;
        bool active;
        Role role;
        uint64 registeredAt;
    }

    mapping(address => Identity) private identities;

    event IdentityRegistered(address indexed account, Role role, uint64 registeredAt);
    event IdentityRoleUpdated(address indexed account, Role previousRole, Role newRole);
    event IdentityDeactivated(address indexed account, address indexed performedBy);
    event IdentityReactivated(address indexed account, address indexed performedBy);

    constructor(address initialAdmin) {
        require(initialAdmin != address(0), "admin is zero address");
        _grantRole(DEFAULT_ADMIN_ROLE, initialAdmin);
        _grantRole(IDENTITY_MANAGER_ROLE, initialAdmin);
    }

    function registerIdentity(address account, Role role)
        external
        onlyRole(IDENTITY_MANAGER_ROLE)
    {
        require(account != address(0), "account is zero address");
        require(!identities[account].registered, "identity already registered");

        uint64 timestamp = uint64(block.timestamp);
        identities[account] = Identity({
            registered: true,
            active: true,
            role: role,
            registeredAt: timestamp
        });

        emit IdentityRegistered(account, role, timestamp);
    }

    function assignRole(address account, Role newRole)
        external
        onlyRole(IDENTITY_MANAGER_ROLE)
    {
        Identity storage identity = identities[account];
        require(identity.registered, "identity not registered");
        require(identity.active, "identity is inactive");

        Role previousRole = identity.role;
        identity.role = newRole;
        emit IdentityRoleUpdated(account, previousRole, newRole);
    }

    function deactivateIdentity(address account)
        external
        onlyRole(IDENTITY_MANAGER_ROLE)
    {
        Identity storage identity = identities[account];
        require(identity.registered, "identity not registered");
        require(identity.active, "identity already inactive");

        identity.active = false;
        emit IdentityDeactivated(account, msg.sender);
    }

    function reactivateIdentity(address account)
        external
        onlyRole(IDENTITY_MANAGER_ROLE)
    {
        Identity storage identity = identities[account];
        require(identity.registered, "identity not registered");
        require(!identity.active, "identity already active");

        identity.active = true;
        emit IdentityReactivated(account, msg.sender);
    }

    function isRegistered(address account) external view returns (bool) {
        return identities[account].registered;
    }

    function isActive(address account) external view returns (bool) {
        return identities[account].registered && identities[account].active;
    }

    function getRole(address account) external view returns (Role) {
        require(identities[account].registered, "identity not registered");
        return identities[account].role;
    }

    function getIdentity(address account)
        external
        view
        returns (bool registered, bool active, Role role, uint64 registeredAt)
    {
        Identity memory identity = identities[account];
        return (identity.registered, identity.active, identity.role, identity.registeredAt);
    }
}
