package com.example.newsagnet.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link UserProfile} verifying guest status, display name resolution, and equality.
 */
public class UserProfileTest {

  @Test
  public void createGuest_defaultsToGuestProperties() {
    UserProfile guest = UserProfile.createGuest();
    assertTrue(guest.isGuest());
    assertEquals(UserProfile.GUEST_ID, guest.getId());
    assertEquals("Guest User", guest.getFormattedDisplayName());
    assertEquals(UserProfile.PROVIDER_GUEST, guest.getAuthProvider());
  }

  @Test
  public void authenticatedUser_resolvesDisplayNameAndEmailPrefix() {
    UserProfile userWithName =
        new UserProfile("u-1", "alice@example.com", "Alice Innovator", UserProfile.PROVIDER_GOOGLE);
    assertFalse(userWithName.isGuest());
    assertEquals("Alice Innovator", userWithName.getFormattedDisplayName());

    UserProfile userWithoutName =
        new UserProfile("u-2", "bob.developer@example.com", null, UserProfile.PROVIDER_GOOGLE);
    assertFalse(userWithoutName.isGuest());
    assertEquals("bob.developer", userWithoutName.getFormattedDisplayName());
  }
}
