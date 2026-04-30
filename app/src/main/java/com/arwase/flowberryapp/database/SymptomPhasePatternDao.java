package com.arwase.flowberryapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.arwase.flowberryapp.models.SymptomPhasePattern;

import java.util.List;

// com.arwase.database.flowberryapp.SymptomPhasePatternDao.java
@Dao
public interface SymptomPhasePatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(SymptomPhasePattern pattern);

    @Delete
    void delete(SymptomPhasePattern pattern);

    @Query("SELECT * FROM symptom_phase_patterns " +
            "WHERE phase = :phaseName")
    List<SymptomPhasePattern> getByPhase(String phaseName);

    @Query("SELECT * FROM symptom_phase_patterns " +
            "WHERE phase = :phaseName AND symptomType = :type " +
            "LIMIT 1")
    SymptomPhasePattern getOne(String phaseName, String type);

    @Query("DELETE FROM symptom_phase_patterns")
    void deleteAll();
}
