package com.example.rpgapp.service;

import android.content.Context;

import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.UserStatistics;
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

                    // Računanje streak-ova - sortiraj po datumu finalizacije
                    int currentStreak = 0;
                    int longestStreak = 0;

                    if (!completedMissions.isEmpty())
                    {
                        // Sortiraj po datumu finalizacije
                        Collections.sort(completedMissions, new Comparator<Mission>() {
                            @Override
                            public int compare(Mission m1, Mission m2) {
                                if (m1.getFinalizationDateTime() == null) return 1;
                                if (m2.getFinalizationDateTime() == null) return -1;
                                return m1.getFinalizationDateTime().compareTo(m2.getFinalizationDateTime());
                            }
                        });

                        int tempStreak = 1;
                        longestStreak = 1;
                        Calendar prevCal = Calendar.getInstance();

                        for (int i = 0; i < completedMissions.size(); i++)
                        {
                            Mission mission = completedMissions.get(i);
                            Date finalizationDate = mission.getFinalizationDateTime();

                            if (finalizationDate != null)
                            {
                                if (i == 0) {
                                    prevCal.setTime(finalizationDate);
                                    continue;
                                }

                                Calendar currentCal = Calendar.getInstance();
                                currentCal.setTime(finalizationDate);

                                // Proveri da li je isti dan ili uzastopni dan
                                long diffInMillis = currentCal.getTimeInMillis() - prevCal.getTimeInMillis();
                                long daysDiff = diffInMillis / (1000 * 60 * 60 * 24);

                                if (daysDiff <= 1)
                                {
                                    // Isti dan ili sledeći dan - nastavi streak
                                    if (daysDiff == 1)
                                    {
                                        tempStreak++;
                                    }
                                }
                                else
                                {
                                    // Prekid streak-a
                                    tempStreak = 1;
                                }

                                if (tempStreak > longestStreak)
                                {
                                    longestStreak = tempStreak;
                                }

                                prevCal.setTime(finalizationDate);
                            }
                        }

                        // Current streak je tempStreak ako se nastavlja do danas
                        currentStreak = tempStreak;
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

                    callback.onResult(statistics);
                })
                .addOnFailureListener(e -> {
                    callback.onResult(new UserStatistics(userId));
                });
    }
}
