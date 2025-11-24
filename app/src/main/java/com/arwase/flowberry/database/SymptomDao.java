package com.arwase.flowberry.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.arwase.flowberry.models.Symptom;

import java.util.Date;
import java.util.List;

@Dao
public interface SymptomDao {

    @Insert
    long insert(Symptom period);

    @Update
    void update(Symptom period);

    @Query("SELECT * FROM symptoms ORDER BY type DESC")
    List<Symptom> getAll();


    // Symptômes entre deux timestamps (début/fin de journée)
    @Query("SELECT * FROM symptoms WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay ORDER BY dateMillis ASC")
    List<Symptom> getByDay(long startOfDay, long endOfDay);


}