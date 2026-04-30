package com.arwase.flowberryapp.logic;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.models.Period;
import com.arwase.flowberryapp.models.Symptom;
import com.arwase.flowberryapp.models.SymptomType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CalendarBackupManager {

    private static final String FORMAT_VERSION = "1";

    private CalendarBackupManager() {
    }

    public static Intent createShareIntent(Context context) throws IOException {
        File backupFile = writeBackupFile(context);
        Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                backupFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.backup_share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.backup_share_message));
        shareIntent.setClipData(ClipData.newRawUri("FlowBerry backup", contentUri));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return Intent.createChooser(shareIntent, context.getString(R.string.backup_share_chooser_title));
    }

    public static ImportResult importFromUri(Context context, Uri uri) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException(context.getString(R.string.backup_import_error_generic));
        }

        JSONObject payload;
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException(context.getString(R.string.backup_import_error_open));
            }
            payload = new JSONObject(readAll(inputStream));
        } catch (JSONException e) {
            throw new IllegalArgumentException(context.getString(R.string.backup_import_error_invalid), e);
        }

        if (!"flowberry-backup".equals(payload.optString("type"))
                || !FORMAT_VERSION.equals(payload.optString("formatVersion"))) {
            throw new IllegalArgumentException(context.getString(R.string.backup_import_error_invalid));
        }

        JSONArray periodsJson = payload.optJSONArray("periods");
        JSONArray symptomsJson = payload.optJSONArray("symptoms");
        JSONArray symptomTypesJson = payload.optJSONArray("symptomTypes");

        if (periodsJson == null || symptomsJson == null || symptomTypesJson == null) {
            throw new IllegalArgumentException(context.getString(R.string.backup_import_error_invalid));
        }

        AppDatabase db = AppDatabase.getInstance(context);

        try {
            db.runInTransaction(() -> {
                try {
                    replaceDatabaseContents(db, periodsJson, symptomsJson, symptomTypesJson);
                } catch (JSONException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof JSONException) {
                throw new IllegalArgumentException(context.getString(R.string.backup_import_error_invalid), e.getCause());
            }
            throw e;
        }

        SymptomPatternTrainer.rebuildAllPatterns(db);

        return new ImportResult(periodsJson.length(), symptomsJson.length(), symptomTypesJson.length());
    }

    private static File writeBackupFile(Context context) throws IOException {
        File shareDir = new File(context.getCacheDir(), "shared");
        if (!shareDir.exists() && !shareDir.mkdirs()) {
            throw new IOException(context.getString(R.string.backup_export_error_directory));
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File targetFile = new File(shareDir, "flowberry-backup-" + timestamp + ".json");
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(targetFile, false),
                StandardCharsets.UTF_8
        )) {
            writer.write(buildBackupJson(context).toString(2));
        } catch (JSONException e) {
            throw new IOException(context.getString(R.string.backup_export_error_encode), e);
        }

        return targetFile;
    }

    private static JSONObject buildBackupJson(Context context) throws JSONException {
        AppDatabase db = AppDatabase.getInstance(context);
        List<Period> periods = db.periodDao().getAll();
        List<Symptom> symptoms = db.symptomDao().getAll();
        List<SymptomType> symptomTypes = db.symptomTypeDao().getAll();

        JSONObject root = new JSONObject();
        root.put("type", "flowberry-backup");
        root.put("formatVersion", FORMAT_VERSION);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("periods", encodePeriods(periods));
        root.put("symptoms", encodeSymptoms(symptoms));
        root.put("symptomTypes", encodeSymptomTypes(symptomTypes));
        return root;
    }

    private static JSONArray encodePeriods(List<Period> periods) throws JSONException {
        JSONArray array = new JSONArray();
        for (Period period : periods) {
            JSONObject item = new JSONObject();
            item.put("id", period.id);
            item.put("startDateMillis", period.startDateMillis);
            if (period.endDateMillis != null) {
                item.put("endDateMillis", period.endDateMillis);
            } else {
                item.put("endDateMillis", JSONObject.NULL);
            }
            item.put("cycleLengthDays", period.cycleLengthDays);
            item.put("periodLengthDays", period.periodLengthDays);
            array.put(item);
        }
        return array;
    }

    private static JSONArray encodeSymptoms(List<Symptom> symptoms) throws JSONException {
        JSONArray array = new JSONArray();
        for (Symptom symptom : symptoms) {
            JSONObject item = new JSONObject();
            item.put("id", symptom.id);
            item.put("dateMillis", symptom.dateMillis);
            item.put("type", symptom.type);
            if (symptom.typeId != null) {
                item.put("typeId", symptom.typeId);
            } else {
                item.put("typeId", JSONObject.NULL);
            }
            item.put("intensity", symptom.intensity);
            item.put("notes", symptom.notes);
            item.put("anticipated", symptom.anticipated);
            item.put("validated", symptom.validated);
            array.put(item);
        }
        return array;
    }

    private static JSONArray encodeSymptomTypes(List<SymptomType> symptomTypes) throws JSONException {
        JSONArray array = new JSONArray();
        for (SymptomType symptomType : symptomTypes) {
            JSONObject item = new JSONObject();
            item.put("id", symptomType.id);
            item.put("name", symptomType.name);
            item.put("category", symptomType.category);
            item.put("userCreated", symptomType.userCreated);
            item.put("archived", symptomType.archived);
            array.put(item);
        }
        return array;
    }

    private static void replaceDatabaseContents(AppDatabase db,
                                                JSONArray periodsJson,
                                                JSONArray symptomsJson,
                                                JSONArray symptomTypesJson) throws JSONException {
        db.symptomPhasePatternDao().deleteAll();
        db.symptomDao().deleteAll();
        db.periodDao().deleteAll();
        db.symptomTypeDao().deleteAll();

        for (int i = 0; i < symptomTypesJson.length(); i++) {
            JSONObject item = symptomTypesJson.getJSONObject(i);
            SymptomType symptomType = new SymptomType();
            symptomType.id = item.optLong("id", 0L);
            symptomType.name = item.optString("name", "");
            symptomType.category = item.isNull("category") ? null : item.optString("category", null);
            symptomType.userCreated = item.optBoolean("userCreated", false);
            symptomType.archived = item.optBoolean("archived", false);
            db.symptomTypeDao().insert(symptomType);
        }

        for (int i = 0; i < periodsJson.length(); i++) {
            JSONObject item = periodsJson.getJSONObject(i);
            Period period = new Period();
            period.id = item.optLong("id", 0L);
            period.startDateMillis = item.optLong("startDateMillis", 0L);
            period.endDateMillis = item.isNull("endDateMillis") ? null : item.optLong("endDateMillis");
            period.cycleLengthDays = item.optInt("cycleLengthDays", 0);
            period.periodLengthDays = item.optInt("periodLengthDays", 0);
            db.periodDao().insert(period);
        }

        for (int i = 0; i < symptomsJson.length(); i++) {
            JSONObject item = symptomsJson.getJSONObject(i);
            Symptom symptom = new Symptom();
            symptom.id = item.optLong("id", 0L);
            symptom.dateMillis = item.optLong("dateMillis", 0L);
            symptom.type = item.optString("type", "");
            symptom.typeId = item.isNull("typeId") ? null : item.optLong("typeId");
            symptom.intensity = item.optInt("intensity", 0);
            symptom.notes = item.isNull("notes") ? null : item.optString("notes", null);
            symptom.anticipated = item.optBoolean("anticipated", false);
            symptom.validated = item.optBoolean("validated", false);
            db.symptomDao().insert(symptom);
        }
    }

    private static String readAll(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    public static final class ImportResult {
        public final int periodsCount;
        public final int symptomsCount;
        public final int symptomTypesCount;

        ImportResult(int periodsCount, int symptomsCount, int symptomTypesCount) {
            this.periodsCount = periodsCount;
            this.symptomsCount = symptomsCount;
            this.symptomTypesCount = symptomTypesCount;
        }
    }
}
