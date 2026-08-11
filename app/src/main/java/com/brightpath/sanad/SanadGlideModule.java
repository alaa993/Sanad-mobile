package com.brightpath.sanad;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/** Required so Glide generates GeneratedAppGlideModule (silences startup warning). */
@GlideModule
public final class SanadGlideModule extends AppGlideModule {
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
