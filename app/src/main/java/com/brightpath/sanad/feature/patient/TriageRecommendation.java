package com.brightpath.sanad.feature.patient;

public class TriageRecommendation {
    public enum Category {
        BIPOLAR, ANXIETY_DEPRESSION, SCHIZOPHRENIA, CHILDREN, MILD, IDENTITY, GENERAL
    }
    public Category category = Category.GENERAL;
    public String suggestedSpecialist;
    public String reasoning;

    public static TriageRecommendation evaluate(PatientIntakeForm form){
        TriageRecommendation rec = new TriageRecommendation();
        if (form == null) return rec;
        boolean longDuration = "more_3".equals(form.duration) || "year".equals(form.duration);
        boolean hasMedication = form.symptoms.contains("medication");
        if (contains(form.triageTags, "bipolar")){
            rec.category = Category.BIPOLAR;
            rec.suggestedSpecialist = "طبيب نفسي";
            rec.reasoning = "الملف يشير إلى أعراض ثنائي القطب وتتطلب مراجعة طبية مباشرة.";
            return rec;
        }
        if (contains(form.triageTags, "schizophrenia")){
            rec.category = Category.SCHIZOPHRENIA;
            rec.suggestedSpecialist = "طبيب نفسي";
            rec.reasoning = "أعراض فصامية تحتاج إشراف طبي مختص.";
            return rec;
        }
        if (contains(form.triageTags, "children")){
            rec.category = Category.CHILDREN;
            rec.suggestedSpecialist = "أخصائي أطفال/سلوك";
            rec.reasoning = "الحالة تخص أطفال أو صعوبات تعلم.";
            return rec;
        }
        if (contains(form.symptoms, "anxiety") || contains(form.symptoms, "depression") || contains(form.triageTags, "anx_dep")) {
            rec.category = Category.ANXIETY_DEPRESSION;
            rec.suggestedSpecialist = longDuration ? "طبيب نفسي" : "أخصائي علاج معرفي";
            rec.reasoning = longDuration ? "الأعراض المزمنة مع القلق/الاكتئاب تتطلب مراجعة طبية." :
                    "الأعراض خفيفة ويمكن البدء بجلسات معرفية سلوكية.";
            return rec;
        }
        if (contains(form.symptoms, "sleep") || contains(form.symptoms, "adhd") || contains(form.triageTags, "mild")){
            rec.category = Category.MILD;
            rec.suggestedSpecialist = "أخصائي نفسي";
            rec.reasoning = "الأعراض توحي باضطرابات خفيفة ويمكن التعامل معها بجلسات دعم.";
            return rec;
        }
        if (contains(form.triageTags, "identity")){
            rec.category = Category.IDENTITY;
            rec.suggestedSpecialist = "أخصائي علاج جدلي/دعم عاطفي";
            rec.reasoning = "تحديات الهوية وفرط المشاعر تحتاج متابعة علاجية متخصصة.";
            return rec;
        }
        if (hasMedication || longDuration){
            rec.category = Category.GENERAL;
            rec.suggestedSpecialist = "طبيب نفسي";
            rec.reasoning = "مدة المشكلة الطويلة أو الحاجة لدواء تتطلب تقييم طبي.";
        } else {
            rec.category = Category.GENERAL;
            rec.suggestedSpecialist = "أخصائي نفسي";
            rec.reasoning = "يمكن البدء بجلسات دعم نفسي عامة.";
        }
        return rec;
    }

    private static boolean contains(java.util.List<String> list, String value){
        if (list == null || value == null) return false;
        for (String s : list){
            if (value.equalsIgnoreCase(s)) return true;
        }
        return false;
    }
}
