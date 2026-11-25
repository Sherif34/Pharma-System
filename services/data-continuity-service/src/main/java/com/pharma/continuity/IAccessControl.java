package com.pharma.continuity;

public interface IAccessControl {
    boolean authorize(String actorId, String permission);
}
