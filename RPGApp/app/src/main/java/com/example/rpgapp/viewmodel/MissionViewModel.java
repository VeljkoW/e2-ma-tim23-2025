package com.example.rpgapp.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.rpgapp.model.Mission;

public class MissionViewModel extends ViewModel {
    public Mission createMission(String id, String name, String description, Mission.FrequencyType frequency, Integer repeatInterval, Mission.RepeatUnit repeatUnit, Mission.Category category, Mission.Difficulty difficulty, Mission.Importance importance, String userId) {
        return new Mission(id, name, description, frequency, repeatInterval, repeatUnit, category, difficulty, importance, userId);
    }
    // Add logic to save mission to repository or Firebase as needed
}

