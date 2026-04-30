package com.arwase.flowberryapp.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.arwase.flowberryapp.models.Period;

import java.util.List;

@Dao
public interface PeriodDao {

    @Insert
    long insert(Period period);

    @Update
    void update(Period period);

    @Delete
    void delete(Period period);

    @Query("DELETE FROM periods")
    void deleteAll();

    @Query("SELECT * FROM periods ORDER BY startDateMillis DESC")
    List<Period> getAll();

    @Query("SELECT * FROM periods WHERE startDateMillis <= :timeMillis ORDER BY startDateMillis DESC LIMIT 1")
    Period getLastPeriodBefore(long timeMillis);

    @Query("SELECT * FROM periods WHERE startDateMillis > :timeMillis ORDER BY startDateMillis ASC LIMIT 1")
    Period getNextPeriodAfter(long timeMillis);

    @Query("SELECT * FROM periods ORDER BY startDateMillis DESC LIMIT 1")
    Period getLastPeriod();

    @Query("SELECT * FROM periods WHERE endDateMillis IS NULL ORDER BY startDateMillis DESC LIMIT 1")
    Period getLastOpenPeriod();

    @Query("SELECT * FROM periods WHERE id = :id LIMIT 1")
    Period getById(long id);

    @Query("SELECT COUNT(*) FROM periods WHERE startDateMillis > 0")
    int getCycleCount();

    @Query("SELECT * FROM periods " +
            "WHERE startDateMillis <= :dayMillis " +
            "AND (endDateMillis IS NULL OR endDateMillis >= :dayMillis) " +
            "LIMIT 1")
    Period findPeriodCovering(long dayMillis);

}
