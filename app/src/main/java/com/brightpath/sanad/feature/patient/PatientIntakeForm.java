package com.brightpath.sanad.feature.patient;

import com.google.gson.annotations.SerializedName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PatientIntakeForm {
    public String fullName;
    public String age;
    // Optional legacy combined age/gender field if provided by backend
    public String ageGender;
    public String occupation;
    public List<String> symptoms = new ArrayList<>();
    public String duration;
    public String primaryIssue;
    public boolean hadConsultation;
    public String consultationNotes;
    public int benefitScore = 50;
    public String notes;
    public List<String> triageTags = new ArrayList<>();
    @SerializedName("triage_category")
    public String triageCategory;
    @SerializedName("triage_recommendation")
    public java.util.Map<String, String> triageRecommendation;
    @SerializedName("referral_physician_recommended")
    public boolean referralPhysicianRecommended;
    @SerializedName("risk_flags")
    public List<String> riskFlags;

    public Map<String, Object> toApiPayload() {
        Map<String, Object> body = new HashMap<>();
        if (fullName != null && !fullName.trim().isEmpty()) {
            body.put("full_name", fullName.trim());
        }
        if (age != null && !age.trim().isEmpty()) {
            try {
                body.put("age", Integer.parseInt(age.trim()));
            } catch (NumberFormatException ignored) {
                // Skip invalid age instead of failing server validation.
            }
        }
        if (occupation != null && !occupation.trim().isEmpty()) {
            body.put("occupation", occupation.trim());
        }
        if (duration != null && !duration.trim().isEmpty()) {
            body.put("issue_duration", duration.trim());
        }
        if (primaryIssue != null && !primaryIssue.trim().isEmpty()) {
            body.put("primary_issue", primaryIssue.trim());
        }
        if (notes != null && !notes.trim().isEmpty()) {
            body.put("notes", notes.trim());
        }
        if (consultationNotes != null && !consultationNotes.trim().isEmpty()) {
            body.put("consult_notes", consultationNotes.trim());
        }
        body.put("previous_consult", hadConsultation);
        body.put("benefit_score", benefitScore);

        Set<String> mergedFlags = new LinkedHashSet<>();
        if (symptoms != null) {
            for (String symptom : symptoms) {
                if (symptom != null && !symptom.trim().isEmpty()) {
                    mergedFlags.add(symptom.trim());
                }
            }
            if (!symptoms.isEmpty()) {
                body.put("symptoms", new ArrayList<>(symptoms));
            }
        }
        if (triageTags != null) {
            for (String tag : triageTags) {
                if (tag != null && !tag.trim().isEmpty()) {
                    mergedFlags.add(tag.trim());
                }
            }
        }
        if (!mergedFlags.isEmpty()) {
            body.put("risk_flags", new ArrayList<>(mergedFlags));
        }
        return body;
    }

    public JSONObject toJson(){
        JSONObject o = new JSONObject();
        try {
            o.put("fullName", fullName);
            o.put("age", age);
            o.put("ageGender", ageGender);
            o.put("occupation", occupation);
            o.put("duration", duration);
            o.put("primaryIssue", primaryIssue);
            o.put("hadConsultation", hadConsultation);
            o.put("consultationNotes", consultationNotes);
            o.put("benefitScore", benefitScore);
            o.put("notes", notes);
            o.put("symptoms", listToJson(symptoms));
            o.put("triageTags", listToJson(triageTags));
        } catch (JSONException ignored) { }
        return o;
    }

    public static PatientIntakeForm fromJson(String raw){
        if (raw == null || raw.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(raw);
            PatientIntakeForm form = new PatientIntakeForm();
            form.fullName = o.optString("fullName", null);
            form.age = o.optString("age", null);
            form.ageGender = o.optString("ageGender", null);
            form.occupation = o.optString("occupation", null);
            form.duration = o.optString("duration", null);
            form.primaryIssue = o.optString("primaryIssue", null);
            form.hadConsultation = o.optBoolean("hadConsultation", false);
            form.consultationNotes = o.optString("consultationNotes", null);
            form.benefitScore = o.optInt("benefitScore", 50);
            form.notes = o.optString("notes", null);
            form.symptoms = jsonToList(o.optJSONArray("symptoms"));
            form.triageTags = jsonToList(o.optJSONArray("triageTags"));
            return form;
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONArray listToJson(List<String> source){
        JSONArray arr = new JSONArray();
        if (source == null) return arr;
        for (String s : source) arr.put(s);
        return arr;
    }

    private static List<String> jsonToList(JSONArray arr){
        List<String> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i=0;i<arr.length();i++){
            list.add(arr.optString(i));
        }
        return list;
    }
}
