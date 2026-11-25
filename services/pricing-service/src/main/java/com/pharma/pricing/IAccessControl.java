package com.pharma.pricing;

public interface IAccessControl {
    boolean checkPermission(String userId, String permission);
}
