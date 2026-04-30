package com.arwase.flowberryapp.database;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.arwase.flowberryapp.models.Period;
import com.arwase.flowberryapp.models.Symptom;
import com.arwase.flowberryapp.models.SymptomPhasePattern;
import com.arwase.flowberryapp.models.SymptomType;

@Database(
        entities = {Period.class, Symptom.class, SymptomType.class, SymptomPhasePattern.class},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    private static final Migration MIGRATION_1_4 = new Migration(1, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            ensureCurrentSchema(database);
        }
    };

    private static final Migration MIGRATION_2_4 = new Migration(2, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            ensureCurrentSchema(database);
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            ensureCurrentSchema(database);
        }
    };

    public abstract PeriodDao periodDao();

    public abstract SymptomDao symptomDao();

    public abstract SymptomTypeDao symptomTypeDao();

    public abstract SymptomPhasePatternDao symptomPhasePatternDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "flowberry-db"
                            )
                            .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void ensureCurrentSchema(SupportSQLiteDatabase database) {
        createPeriodsTableIfNeeded(database);
        createSymptomsTableIfNeeded(database);
        createSymptomTypesTableIfNeeded(database);
        createSymptomPhasePatternsTableIfNeeded(database);

        addColumnIfMissing(database, "periods", "endDateMillis", "ALTER TABLE periods ADD COLUMN endDateMillis INTEGER");
        addColumnIfMissing(database, "periods", "cycleLengthDays", "ALTER TABLE periods ADD COLUMN cycleLengthDays INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(database, "periods", "periodLengthDays", "ALTER TABLE periods ADD COLUMN periodLengthDays INTEGER NOT NULL DEFAULT 0");

        addColumnIfMissing(database, "symptoms", "type", "ALTER TABLE symptoms ADD COLUMN type TEXT");
        addColumnIfMissing(database, "symptoms", "typeId", "ALTER TABLE symptoms ADD COLUMN typeId INTEGER");
        addColumnIfMissing(database, "symptoms", "intensity", "ALTER TABLE symptoms ADD COLUMN intensity INTEGER NOT NULL DEFAULT 3");
        addColumnIfMissing(database, "symptoms", "notes", "ALTER TABLE symptoms ADD COLUMN notes TEXT");
        addColumnIfMissing(database, "symptoms", "anticipated", "ALTER TABLE symptoms ADD COLUMN anticipated INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(database, "symptoms", "validated", "ALTER TABLE symptoms ADD COLUMN validated INTEGER NOT NULL DEFAULT 1");

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_symptom_phase_patterns_symptomType_phase` ON `symptom_phase_patterns` (`symptomType`, `phase`)");
    }

    private static void createPeriodsTableIfNeeded(SupportSQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `periods` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startDateMillis` INTEGER NOT NULL, " +
                        "`endDateMillis` INTEGER, " +
                        "`cycleLengthDays` INTEGER NOT NULL DEFAULT 0, " +
                        "`periodLengthDays` INTEGER NOT NULL DEFAULT 0)"
        );
    }

    private static void createSymptomsTableIfNeeded(SupportSQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `symptoms` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`type` TEXT, " +
                        "`typeId` INTEGER, " +
                        "`intensity` INTEGER NOT NULL DEFAULT 3, " +
                        "`notes` TEXT, " +
                        "`anticipated` INTEGER NOT NULL DEFAULT 0, " +
                        "`validated` INTEGER NOT NULL DEFAULT 1)"
        );
    }

    private static void createSymptomTypesTableIfNeeded(SupportSQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `symptom_types` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`category` TEXT, " +
                        "`userCreated` INTEGER NOT NULL, " +
                        "`archived` INTEGER NOT NULL)"
        );
    }

    private static void createSymptomPhasePatternsTableIfNeeded(SupportSQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `symptom_phase_patterns` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`symptomType` TEXT NOT NULL, " +
                        "`phase` TEXT NOT NULL, " +
                        "`typicalIntensity` INTEGER NOT NULL, " +
                        "`cyclesWithSymptom` INTEGER NOT NULL)"
        );
    }

    private static void addColumnIfMissing(SupportSQLiteDatabase database,
                                           String tableName,
                                           String columnName,
                                           String sql) {
        if (!hasColumn(database, tableName, columnName)) {
            database.execSQL(sql);
        }
    }

    private static boolean hasColumn(SupportSQLiteDatabase database, String tableName, String columnName) {
        try (Cursor cursor = database.query("PRAGMA table_info(`" + tableName + "`)")) {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && columnName.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        }
    }
}
