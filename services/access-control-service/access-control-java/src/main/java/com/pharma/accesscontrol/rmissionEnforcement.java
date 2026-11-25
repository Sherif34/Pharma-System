package com.pharma.accesscontrol;

public interface IPermissionEnforcement {
    boolean hasPermission(String userId, String permission);
}
