package com.arwase.flowberryapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.arwase.flowberryapp.models.SymptomType;

import java.util.List;

@Dao
public interface SymptomTypeDao {

    @Insert
    long insert(SymptomType type);

    @Update
    void update(SymptomType type);

    @Delete
    void delete(SymptomType type);

    @Query("DELETE FROM symptom_types")
    void deleteAll();

    @Query("SELECT * FROM symptom_types WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    List<SymptomType> getAllActive();

    @Query("SELECT * FROM symptom_types ORDER BY name COLLATE NOCASE ASC")
    List<SymptomType> getAll();

    @Query("SELECT * FROM symptom_types WHERE id = :id LIMIT 1")
    SymptomType getById(long id);

    @Query("SELECT * FROM symptom_types WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    SymptomType getByName(String name);
}
