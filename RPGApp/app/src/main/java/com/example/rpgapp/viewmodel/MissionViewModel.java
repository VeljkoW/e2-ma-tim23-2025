package com.example.rpgapp.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.rpgapp.model.Mission;
import java.util.Date;

public class MissionViewModel extends ViewModel {
    public Mission createMission(String id, String name, String description, Mission.FrequencyType frequency, Integer repeatInterval, Mission.RepeatUnit repeatUnit, String categoryId, Mission.Difficulty difficulty, Mission.Importance importance, String userId, int userLevel, Date dueDateTime) {
        return new Mission(id, name, description, frequency, repeatInterval, repeatUnit, categoryId, difficulty, importance, userId, userLevel, dueDateTime);
    }
    // Add logic to save mission to repository or Firebase as needed
}
