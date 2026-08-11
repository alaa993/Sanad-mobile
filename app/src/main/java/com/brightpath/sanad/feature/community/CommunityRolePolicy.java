package com.brightpath.sanad.feature.community;

import androidx.annotation.Nullable;

import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** صلاحيات واجهة المجتمع حسب الدور. */
public final class CommunityRolePolicy {

    public final String role;

    public CommunityRolePolicy(@Nullable String rawRole) {
        String r = rawRole != null ? rawRole.trim().toLowerCase(Locale.ROOT) : "";
        if (r.contains("admin")) {
            this.role = "admin";
        } else if (r.contains("specialist") || r.contains("therapist") || r.contains("counselor")) {
            this.role = "specialist";
        } else if (r.contains("organization") || r.equals("org")) {
            this.role = "organization";
        } else if (r.contains("patient") || r.isEmpty()) {
            // Unknown / empty defaults to patient only for patient-facing apps after login.
            // Patient-only CTAs still require an explicit patient role when possible.
            this.role = r.isEmpty() ? "patient" : "patient";
        } else {
            this.role = r;
        }
    }

    public boolean isPatient() {
        return "patient".equals(role);
    }

    public boolean isSpecialist() {
        return "specialist".equals(role);
    }

    public boolean showsSearch() {
        return "patient".equals(role) || "specialist".equals(role) || "admin".equals(role);
    }

    public boolean showsFilters() {
        return "patient".equals(role);
    }

    public boolean showsPublicFeedCta() {
        return !"organization".equals(role);
    }

    public boolean showsVent() {
        return "patient".equals(role);
    }

    public boolean showsAnonymousMatch() {
        return "patient".equals(role);
    }

    public boolean showsCoach() {
        return "patient".equals(role);
    }

    public boolean showsSafePlace() {
        return "patient".equals(role);
    }

    public boolean canJoinFreely() {
        return !"organization".equals(role);
    }

    public String defaultPostType() {
        if ("patient".equals(role)) return "personal";
        if ("admin".equals(role)) return "official";
        return "awareness";
    }

    public boolean canPost(@Nullable CommunityModels.Community community) {
        if (community == null || !community.joined) return false;
        if ("organization".equals(role)) {
            return community.organization_owned;
        }
        return true;
    }

    public boolean canAnswerQa() {
        return "specialist".equals(role) || "admin".equals(role) || "organization".equals(role);
    }

    public boolean canCreateCommunity() {
        return "admin".equals(role) || "organization".equals(role);
    }

    public int screenTitleRes() {
        if ("specialist".equals(role)) return R.string.community_header_title_specialist;
        if ("organization".equals(role)) return R.string.community_header_title_org;
        if ("admin".equals(role)) return R.string.community_header_title_admin;
        return R.string.community_header_title;
    }

    public int screenSubtitleRes() {
        if ("specialist".equals(role)) return R.string.community_header_subtitle_specialist;
        if ("organization".equals(role)) return R.string.community_header_subtitle_org;
        if ("admin".equals(role)) return R.string.community_header_subtitle_admin;
        return R.string.community_header_subtitle;
    }

    public int ctaTitleRes() {
        return "specialist".equals(role)
                ? R.string.community_specialist_cta_title
                : R.string.community_cta_title;
    }

    public int ctaSubtitleRes() {
        return "specialist".equals(role)
                ? R.string.community_specialist_cta_subtitle
                : R.string.community_cta_subtitle;
    }

    public List<CommunityModels.Community> filter(List<CommunityModels.Community> input) {
        List<CommunityModels.Community> out = new ArrayList<>();
        if (input == null) return out;
        for (CommunityModels.Community c : input) {
            if (c == null) continue;
            if ("organization".equals(role)) {
                if (c.organization_owned || (c.slug != null && c.slug.startsWith("org-support"))) out.add(c);
            } else if ("patient".equals(role)) {
                if (!c.organization_owned) out.add(c);
            } else {
                out.add(c);
            }
        }
        return out;
    }
}
