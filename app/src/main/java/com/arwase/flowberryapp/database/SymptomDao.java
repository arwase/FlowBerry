package com.arwase.flowberryapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.arwase.flowberryapp.models.Symptom;

import java.util.List;

@Dao
public interface SymptomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Symptom symptom);

    @Update
    void update(Symptom symptom);

    @Delete
    void delete(Symptom symptom);

    @Query("DELETE FROM symptoms")
    void deleteAll();

    @Query("SELECT * FROM symptoms " +
            "WHERE dateMillis >= :startOfDay AND dateMillis <= :endOfDay " +
            "ORDER BY dateMillis ASC")
    List<Symptom> getByDay(long startOfDay, long endOfDay);


    @Query("SELECT * FROM symptoms WHERE id = :id LIMIT 1")
    Symptom getById(long id);

    @Query("SELECT * FROM symptoms WHERE anticipated = 0 ORDER BY dateMillis ASC, id ASC")
    List<Symptom> getAllReal();

    @Query("SELECT * FROM symptoms ORDER BY dateMillis ASC, id ASC")
    List<Symptom> getAll();

    @Query("SELECT COUNT(*) FROM symptoms " +
            "WHERE type = :type " +
            "AND anticipated = :anticipated " +
            "AND dateMillis >= :startMillis " +
            "AND dateMillis <= :endMillis")
    int countTypeInRange(String type,
                         boolean anticipated,
                         long startMillis,
                         long endMillis);
}
