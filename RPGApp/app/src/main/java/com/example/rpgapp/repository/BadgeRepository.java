package com.example.rpgapp.repository;

import com.example.rpgapp.model.Badge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BadgeRepository {
    private Map<String, Badge> allBadges;
    private List<Badge> userBadges;

    public BadgeRepository() {
        this.allBadges = new HashMap<>();
        this.userBadges = new ArrayList<>();
        initializeDefaultBadges();
    }

    /**
     * Initialize some default badges in the system
     */
    private void initializeDefaultBadges() {
        Badge firstLogin = new Badge("first_login", "First Login", "Welcome to the game!");
        Badge levelUp = new Badge("level_up", "Level Up", "Reached a new level");
        Badge achiever = new Badge("achiever", "Achiever", "Completed your first quest");

        allBadges.put(firstLogin.getId(), firstLogin);
        allBadges.put(levelUp.getId(), levelUp);
        allBadges.put(achiever.getId(), achiever);
    }

    /**
     * Get all available badges in the system
     */
    public List<Badge> getAllBadges() {
        return new ArrayList<>(allBadges.values());
    }

    /**
     * Get a specific badge by ID
     */
    public Badge getBadgeById(String badgeId) {
        return allBadges.get(badgeId);
    }

    /**
     * Get all badges awarded to a specific user
     */
    public List<Badge> getBadgesForUser(String userId) {
        return userBadges.stream()
                .filter(badge -> userId.equals(badge.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * Award a badge to a user
     */
    public boolean awardBadgeToUser(String badgeId, String userId) {
        Badge templateBadge = allBadges.get(badgeId);
        if (templateBadge == null) {
            return false; // Badge doesn't exist
        }

        // Check if user already has this badge
        boolean alreadyHasBadge = userBadges.stream()
                .anyMatch(badge -> badgeId.equals(badge.getId()) && userId.equals(badge.getUserId()));

        if (alreadyHasBadge) {
            return false; // User already has this badge
        }

        // Create a new badge instance for the user
        Badge userBadge = new Badge(templateBadge.getId(), templateBadge.getName(),
                                   templateBadge.getDescription(), templateBadge.getIconResource());
        userBadge.setUserId(userId);

        userBadges.add(userBadge);
        return true;
    }

    /**
     * Check if a user has a specific badge
     */
    public boolean userHasBadge(String badgeId, String userId) {
        return userBadges.stream()
                .anyMatch(badge -> badgeId.equals(badge.getId()) && userId.equals(badge.getUserId()));
    }


    public int getBadgeCountForUser(String userId) {
        return (int) userBadges.stream()
                .filter(badge -> userId.equals(badge.getUserId()))
                .count();
    }

    /**
     * Remove a badge from a user (if needed for testing or admin purposes)
     */
    public boolean removeBadgeFromUser(String badgeId, String userId) {
        return userBadges.removeIf(badge -> badgeId.equals(badge.getId()) && userId.equals(badge.getUserId()));
    }
}
