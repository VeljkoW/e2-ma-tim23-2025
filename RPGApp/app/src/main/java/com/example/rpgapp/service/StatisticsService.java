package com.example.rpgapp.service;

import android.content.Context;

import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserStatistics;
import com.example.rpgapp.model.AllianceBoss;
import com.example.rpgapp.model.Alliance;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsService {

    private final FirebaseFirestore db;
    private final Context context;

    public StatisticsService(Context context)
    {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    public void loadStatistics(String userId, AuthCallback<UserStatistics> callback)
    {
        calculateStatisticsFromMissions(userId, callback);
    }

    public void calculateStatisticsFromMissions(String userId, AuthCallback<UserStatistics> callback)
    {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    User user = userDoc.toObject(User.class);
                    int activeDaysStreak = user != null ? user.getActiveDaysStreak() : 0;

                    // Prvo učitaj kategorije
                    db.collection("categories")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(categorySnapshots -> {
                                Map<String, String> categoryNames = new HashMap<>();
                                Map<String, Integer> categoryColors = new HashMap<>();

                                for (QueryDocumentSnapshot doc : categorySnapshots) {
                                    Category category = doc.toObject(Category.class);
                                    if (category != null)
                                    {
                                        categoryNames.put(category.getId(), category.getName());
                                        categoryColors.put(category.getId(), category.getColor());
                                    }
                                }

                                // Sada učitaj misije
                                loadMissionsAndCalculate(userId, activeDaysStreak, categoryNames, categoryColors, callback);
                            })
                            .addOnFailureListener(e -> {
                                loadMissionsAndCalculate(userId, activeDaysStreak, new HashMap<>(), new HashMap<>(), callback);
                            });
                })
                .addOnFailureListener(e -> {
                    callback.onResult(new UserStatistics(userId));
                });
    }

    private void loadMissionsAndCalculate(String userId, int activeDaysStreak,
                                          Map<String, String> categoryNames,
                                          Map<String, Integer> categoryColors,
                                          AuthCallback<UserStatistics> callback) {
        db.collection("missions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    UserStatistics statistics = new UserStatistics(userId);

                    int totalCreated = 0;
                    int totalCompleted = 0;
                    int totalFailed = 0;
                    int totalCancelled = 0;
                    Map<String, Integer> categoryMap = new HashMap<>();
                    Map<String, Integer> xpLast7DaysMap = new HashMap<>();
                    Map<String, List<Integer>> xpByDate = new HashMap<>(); // Za računanje proseka
                    int totalXPForAverage = 0;

                    // Lista za sortiranje Mission-a po datumu finalizacije za streak računanje
                    List<Mission> completedMissions = new ArrayList<>();

                    // Postavi datum za poslednjih 7 dana
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    Date sevenDaysAgo = cal.getTime();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Mission mission = document.toObject(Mission.class);
                        if (mission != null) {
                            totalCreated++;

                            // Broji po statusu
                            if (mission.getStatus() == Mission.Status.COMPLETED)
                            {
                                totalCompleted++;
                                totalXPForAverage += mission.getTotalXP();
                                completedMissions.add(mission);

                                // Broji po kategorijama
                                String categoryId = mission.getCategoryId();
                                if (categoryId != null)
                                {
                                    categoryMap.put(categoryId, categoryMap.getOrDefault(categoryId, 0) + 1);
                                }

                                // XP za poslednjih 7 dana
                                Date completionDate = mission.getFinalizationDateTime();
                                if (completionDate != null && completionDate.after(sevenDaysAgo))
                                {
                                    String dateKey = sdf.format(completionDate);
                                    xpLast7DaysMap.put(dateKey, xpLast7DaysMap.getOrDefault(dateKey, 0) + mission.getTotalXP());
                                }

                                // Sakupljaj XP po datumu za prosečnu težinu
                                if (completionDate != null) {
                                    String dateKey = sdf.format(completionDate);
                                    if (!xpByDate.containsKey(dateKey)) {
                                        xpByDate.put(dateKey, new ArrayList<>());
                                    }
                                    xpByDate.get(dateKey).add(mission.getTotalXP());
                                }
                            }
                            else if (mission.getStatus() == Mission.Status.FAILED)
                            {
                                totalFailed++;
                            }
                            else if (mission.getStatus() == Mission.Status.CANCELLED)
                            {
                                totalCancelled++;
                            }
                        }
                    }

                    // Računanje prosečne težine po danima
                    Map<String, Float> averageDifficultyMap = new HashMap<>();
                    for (Map.Entry<String, List<Integer>> entry : xpByDate.entrySet()) {
                        List<Integer> xpList = entry.getValue();
                        float sum = 0;
                        for (Integer xp : xpList) {
                            sum += xp;
                        }
                        float average = xpList.size() > 0 ? sum / xpList.size() : 0;
                        averageDifficultyMap.put(entry.getKey(), average);
                    }

                    // Računanje streak-ova - streak se prekida samo sa FAILED misijama
                    int currentStreak = 0;
                    int longestStreak = 0;

                    // Sakupi sve misije (completed i failed) i sortiraj po datumu
                    List<Mission> allMissions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots)
                    {
                        Mission mission = document.toObject(Mission.class);
                        if (mission != null && mission.getFinalizationDateTime() != null &&
                                (mission.getStatus() == Mission.Status.COMPLETED || mission.getStatus() == Mission.Status.FAILED)) {
                            allMissions.add(mission);
                        }
                    }

                    if (!allMissions.isEmpty())
                    {
                        Collections.sort(allMissions, new Comparator<Mission>() {
                            @Override
                            public int compare(Mission m1, Mission m2) {
                                if (m1.getFinalizationDateTime() == null) return 1;
                                if (m2.getFinalizationDateTime() == null) return -1;
                                return m1.getFinalizationDateTime().compareTo(m2.getFinalizationDateTime());
                            }
                        });

                        // Grupiši misije po danima
                        Map<String, List<Mission>> missionsByDay = new HashMap<>();
                        for (Mission mission : allMissions) {
                            String dayKey = sdf.format(mission.getFinalizationDateTime());
                            if (!missionsByDay.containsKey(dayKey)) {
                                missionsByDay.put(dayKey, new ArrayList<>());
                            }
                            missionsByDay.get(dayKey).add(mission);
                        }

                        // Izvuci sortirane datume
                        List<String> sortedDates = new ArrayList<>(missionsByDay.keySet());
                        Collections.sort(sortedDates);

                        int tempStreak = 0;
                        String lastStreakDate = null;

                        for (String dateKey : sortedDates) {
                            List<Mission> dayMissions = missionsByDay.get(dateKey);

                            // Proveri da li ima FAILED misiju tog dana
                            boolean hasFailed = false;
                            for (Mission m : dayMissions) {
                                if (m.getStatus() == Mission.Status.FAILED) {
                                    hasFailed = true;
                                    break;
                                }
                            }

                            if (hasFailed) {
                                // FAILED misija prekida streak
                                if (tempStreak > longestStreak) {
                                    longestStreak = tempStreak;
                                }
                                tempStreak = 0;
                                lastStreakDate = null;
                            } else {
                                // Dan sa COMPLETED misijama (bez FAILED)
                                tempStreak++;
                                lastStreakDate = dateKey;

                                if (tempStreak > longestStreak) {
                                    longestStreak = tempStreak;
                                }
                            }
                        }

                        // Current streak - proveri da li se nastavlja do danas ili juče
                        if (lastStreakDate != null && tempStreak > 0) {
                            try {
                                Calendar lastStreakCal = Calendar.getInstance();
                                lastStreakCal.setTime(sdf.parse(lastStreakDate));

                                Calendar todayCal = Calendar.getInstance();
                                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                                todayCal.set(Calendar.MINUTE, 0);
                                todayCal.set(Calendar.SECOND, 0);
                                todayCal.set(Calendar.MILLISECOND, 0);

                                Calendar yesterdayCal = (Calendar) todayCal.clone();
                                yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);

                                lastStreakCal.set(Calendar.HOUR_OF_DAY, 0);
                                lastStreakCal.set(Calendar.MINUTE, 0);
                                lastStreakCal.set(Calendar.SECOND, 0);
                                lastStreakCal.set(Calendar.MILLISECOND, 0);

                                if (lastStreakCal.equals(todayCal) || lastStreakCal.equals(yesterdayCal)) {
                                    currentStreak = tempStreak;
                                } else {
                                    currentStreak = 0;
                                }
                            } catch (Exception e) {
                                currentStreak = 0;
                            }
                        } else {
                            currentStreak = 0;
                        }
                    }

                    // Ažuriraj statistics objekat
                    statistics.setActiveDaysStreak(activeDaysStreak);
                    statistics.setTotalTasksCreated(totalCreated);
                    statistics.setTotalTasksCompleted(totalCompleted);
                    statistics.setTotalTasksIncomplete(totalFailed);
                    statistics.setTotalTasksCancelled(totalCancelled);
                    statistics.setTasksCompletedByCategory(categoryMap);
                    statistics.setCategoryNames(categoryNames);
                    statistics.setCategoryColors(categoryColors);
                    statistics.setXpLast7Days(xpLast7DaysMap);
                    statistics.setAverageDifficultyOverTime(averageDifficultyMap);
                    statistics.setCurrentTaskStreak(currentStreak);
                    statistics.setLongestTaskStreak(longestStreak);

                    // Učitaj podatke o AllianceBoss-ovima
                    loadAllianceBossStatistics(userId, statistics, callback);
                })
                .addOnFailureListener(e -> {
                    callback.onResult(new UserStatistics(userId));
                });
    }

    private void loadAllianceBossStatistics(String userId, UserStatistics statistics, AuthCallback<UserStatistics> callback) {
        // Prvo pronađi sve saveze u kojima je korisnik član
        db.collection("alliances")
                .get()
                .addOnSuccessListener(allianceSnapshots -> {
                    List<String> userAllianceIds = new ArrayList<>();

                    // Pronađi sve saveze gde je korisnik član
                    for (QueryDocumentSnapshot doc : allianceSnapshots) {
                        Alliance alliance = doc.toObject(Alliance.class);
                        if (alliance != null && alliance.getMemberIds() != null &&
                            alliance.getMemberIds().contains(userId)) {
                            userAllianceIds.add(alliance.getId());
                        }
                    }

                    if (userAllianceIds.isEmpty()) {
                        // Korisnik nije u nijednom savezu
                        statistics.setSpecialMissionsStarted(0);
                        statistics.setSpecialMissionsCompleted(0);
                        callback.onResult(statistics);
                        return;
                    }

                    // Sada učitaj sve AllianceBoss-ove za te saveze
                    db.collection("allianceBosses")
                            .get()
                            .addOnSuccessListener(bossSnapshots -> {
                                int totalStarted = 0;
                                int totalCompleted = 0;

                                for (QueryDocumentSnapshot doc : bossSnapshots) {
                                    AllianceBoss boss = doc.toObject(AllianceBoss.class);
                                    if (boss != null && boss.getAllianceId() != null &&
                                        userAllianceIds.contains(boss.getAllianceId())) {
                                        // Ovo je boss iz saveza u kojem je korisnik bio član
                                        totalStarted++;

                                        if (boss.getStatus() == AllianceBoss.Status.DEAD) {
                                            totalCompleted++;
                                        }
                                    }
                                }

                                statistics.setSpecialMissionsStarted(totalStarted);
                                statistics.setSpecialMissionsCompleted(totalCompleted);
                                callback.onResult(statistics);
                            })
                            .addOnFailureListener(e -> {
                                // Ako ne uspe učitavanje boss-ova, postavi na 0
                                statistics.setSpecialMissionsStarted(0);
                                statistics.setSpecialMissionsCompleted(0);
                                callback.onResult(statistics);
                            });
                })
                .addOnFailureListener(e -> {
                    // Ako ne uspe učitavanje saveza, postavi na 0
                    statistics.setSpecialMissionsStarted(0);
                    statistics.setSpecialMissionsCompleted(0);
                    callback.onResult(statistics);
                });
    }
}
